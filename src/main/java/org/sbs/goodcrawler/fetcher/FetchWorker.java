/**
 * ##########################  GoodCrawler  ############################
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sbs.goodcrawler.fetcher;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.sbs.crawler.Worker;
import org.sbs.goodcrawler.conf.jobconf.FetchConfig;
import org.sbs.goodcrawler.exception.QueueException;
import org.sbs.goodcrawler.job.Page;
import org.sbs.goodcrawler.job.Parser;
import org.sbs.goodcrawler.urlmanager.BloomfilterHelper;
import org.sbs.goodcrawler.urlmanager.PendingUrls;
import org.sbs.goodcrawler.urlmanager.WebURL;
import org.sbs.robotstxt.RobotstxtConfig;
import org.sbs.robotstxt.RobotstxtServer;
import org.sbs.util.UrlUtils;

import com.google.common.collect.Lists;

/**
 * @author shenbaise(shenbaise@outlook.com)
 * @date 2013-7-1
 * 页面抓取工
 */
public abstract class FetchWorker extends Worker {
	private Log log = LogFactory.getLog(this.getClass());
	protected UrlUtils urlUtils = new UrlUtils();
	protected BloomfilterHelper bloomfilterHelper = BloomfilterHelper.getInstance();
	/**
	 * url队列
	 */
	protected PendingUrls pendingUrls = PendingUrls.getInstance();
	/**
	 * Page队列
	 */
	protected PendingPages pendingPages = PendingPages.getInstace();
	/**
	 * 爬取器
	 */
	protected PageFetcher fetcher;
	/**
	 * job配置
	 */
	protected FetchConfig conf;
	/**
	 * 解析器
	 */
	protected Parser parser;
	/**
	 * robots
	 */
	public static RobotstxtServer robotstxtServer;
	
	public List<Pattern> fetchFilters = Lists.newArrayList();
	
	public List<Pattern> extractFilters = Lists.newArrayList();
	
	/**
	 * @param conf
	 * 构造函数，未提供爬取器，需通过setFetcher方法设置Fetcher
	 */
	public FetchWorker(FetchConfig conf){
		this.conf = conf;
		parser = new Parser(conf);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		robotstxtConfig.setCacheSize(1000);
		robotstxtConfig.setEnabled(conf.isRobots());
		robotstxtConfig.setUserAgentName(conf.getAgent());
		robotstxtServer = new RobotstxtServer(robotstxtConfig, fetcher);
		
		// 过滤器
		List<String> urls1 = conf.getFetchUrlFilters();
		List<String> urls2 = conf.getExtractUrlfilters();
		for(String s:urls1){
			fetchFilters.add(Pattern.compile(s));
		}
		for(String s:urls2){
			extractFilters.add(Pattern.compile(s));
		}
	}
	/**
	 * @param conf
	 * @param fetcher 推荐使用的构造函数
	 */
	public FetchWorker(FetchConfig conf,PageFetcher fetcher){
		this.fetcher = fetcher;
		this.conf = conf;
		parser = new Parser(conf);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		robotstxtConfig.setCacheSize(1000);
		robotstxtConfig.setEnabled(conf.isRobots());
		robotstxtConfig.setUserAgentName(conf.getAgent());
		robotstxtServer = new RobotstxtServer(robotstxtConfig, fetcher);
		// 过滤器
		List<String> urls1 = conf.getFetchUrlFilters();
		List<String> urls2 = conf.getExtractUrlfilters();
		for(String s:urls1){
			fetchFilters.add(Pattern.compile(s));
		}
		for(String s:urls2){
			extractFilters.add(Pattern.compile(s));
		}
		
	}
	public FetchWorker setFetcher(final PageFetcher fetcher){
		this.fetcher = fetcher;
		return this;
	}
	/**
	 * @desc 工作成功
	 */
	public abstract void onSuccessed();
	/**
	 * @desc 工作失败
	 */
	public abstract void onFailed(WebURL url);
	/**
	 * @desc 忽略工作
	 */
	public abstract void onIgnored(WebURL url);
	/**
	 * fetcher filter
	 * @param url
	 * @return
	 */
	public boolean fetchFilter(String url){
		
		if(null==fetchFilters || fetchFilters.size()==0){
			return true;
		}
		for(Pattern p:fetchFilters){
			if(p.matcher(url).matches()){
				return true;
			}
		}
		return false;
	}
	/**
	 * extract filter
	 * @param url
	 * @return
	 */
	public boolean extractFilter(String url){
		// bloomfilter it
		if(null==extractFilters || extractFilters.size()==0){
			return true;
		}
		for(Pattern p:extractFilters){
			if(p.matcher(url).matches()){
				return true;
			}
		}
		return false;
	}
	/**
	 * @param url
	 * @desc 爬网页
	 */
	public void fetchPage(WebURL url){
		PageFetchResult result = null;
		if(null!=url && StringUtils.isNotBlank(url.getURL())){
			// 是否需要爬
			if(fetchFilter(url.getURL())){
				result = fetcher.fetchHeader(url);
				// 获取状态
				int statusCode = result.getStatusCode();
				if (statusCode == CustomFetchStatus.PageTooBig) {
					onIgnored(url);
					return ;
				}
				if (statusCode != HttpStatus.SC_OK){
					onFailed(url);
				}else {
					Page page = new Page(url);
					pendingUrls.processedSuccess();
					if (!result.fetchContent(page)) {
						onFailed(url);
						return;
					}
					if (!parser.parse(page, url.getURL())) {
						onFailed(url);
						return;
					}
					try {
						// 是否加入抽取队列
						if(extractFilter(url.getURL())){
							pendingPages.addPage(page);
						}
//						else {
//							onIgnored(url);
//						}
						
						// 提取Url，放入待抓取Url队列
						Document doc = Jsoup.parse(new String(page.getContentData(),page.getContentCharset()), urlUtils.getBaseUrl(page.getWebURL().getURL()));
						Elements links = doc.getElementsByTag("a"); 
				        if (!links.isEmpty()) { 
				            for (Element link : links) { 
				                String linkHref = link.absUrl("href"); 
				                // 是否加入爬取队列
				                if(fetchFilter(linkHref) 
				                		&& !bloomfilterHelper.exist(linkHref)){
				                	WebURL purl = new WebURL();
				                	purl.setURL(linkHref);
				                	purl.setJobName(conf.jobName);
				                	try {
										if(!pendingUrls.addUrl(purl,1000)){
											FileUtils.writeStringToFile(new File("status/_urls.good"), url.getURL()+"\n", true);
										}
									} catch (QueueException e) {
										 log.error(e.getMessage());
									}
				                }
				            }
				        }
					} catch (QueueException e) {
						log.warn("一个页面加入待处理队列时失败" + e.getMessage());
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				onIgnored(url);
			}
		}
	}
	
	public static void main(String[] args) {
	}
}