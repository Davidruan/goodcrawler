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
package org.sbs.goodcrawler.plugin.storage;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.print.attribute.standard.JobName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.sbs.goodcrawler.jobconf.StoreConfig;
import org.sbs.goodcrawler.page.ExtractedPage;
import org.sbs.goodcrawler.plugin.EsClient;
import org.sbs.goodcrawler.plugin.storage.p.IESStoragePlugin;
import org.sbs.goodcrawler.storage.Storage;
import org.sbs.goodcrawler.storage.StoreResult;
import org.sbs.goodcrawler.storage.StoreResult.Status;
import org.sbs.url.WebURL;
import org.sbs.util.MD5Utils;
import org.sbs.util.MapUtils;

import com.google.common.collect.Maps;

/**
 * @author shenbaise(shenbaise@outlook.com)
 * @date 2013-6-30
 * 存储到es
 */
@SuppressWarnings("rawtypes")
public class ElasticSearchStorage extends Storage {
	private Log log = LogFactory.getLog(this.getClass());
	Client client = EsClient.getClient();
	
	private StoreConfig config;
	public ElasticSearchStorage(StoreConfig config,IESStoragePlugin plugin){
		this.config = config;
		this.plugin = plugin;
	}
	private IESStoragePlugin plugin = null;
	
	@Override
	public StoreResult beforeStore() {
		return null;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public StoreResult onStore(ExtractedPage page) {
		System.out.println("start..");
		StoreResult storeResult = null;
		if(null!=plugin){
			page=plugin.process(page);
		}
		System.out.println("x1");
		try {
			storeResult = new StoreResult();
			// 处理Result
			if(null==page || page.getMessages()==null){
				storeResult.setStatus(Status.ignored);
				return storeResult;
			}
			HashMap<String, Object> data = page.getMessages();
			HashMap<String, Object> content = (HashMap<String, Object>) data.get(config.indexName);
			content.put("url", page.getUrl().getURL());
			System.out.println("x2");
			// 判断是否需要更新url--重新爬去
			String update = (String) content.get("update");
			if(null!=update){
				System.out.println("x3");
				if(update.contains("更新")){
					System.out.println("x4");
					// 发送到update url index
					HashMap<String, Object> m = Maps.newHashMap();
					m.put("url", page.getUrl().getURL());
					m.put("domain", page.getUrl().getDomain());
					m.put("time", new Date());
					m.put("indexName", config.indexName);
					m.put("jobName", config.jobName);
					EsClient.index("update", "0", m);
					System.err.println("####################################");
					System.err.println(content);
					if("电影".equals((String)content.get("category"))){
						System.out.println("no");
					}
					System.out.println("x5");
				}
			}
			System.out.println("start..2");
			// 判断重爬url是否已经完全更新
			if(StringUtils.isBlank(update) && page.getUrl().isRecraw()){
				EsClient.delete(config.indexName, MD5Utils.createMD5(page.getUrl().getURL()));
			}
			content.remove("update");
			// 判断是否已存在
			GetResponse get = null;
			try {
				get = client.prepareGet(config.indexName, "0",MD5Utils.createMD5((String)content
						.get("url")))
						.execute()
						.actionGet();
			} catch (Exception e) {
				e.printStackTrace();
				// index不存在？？
				EsClient.index(config.indexName, "0", content);
			}
			System.out.println("start..3");
			if(null!=get && get.isExists()){
				Map<String, Object> m = get.getSource();
				m = MapUtils.mager((HashMap<String, Object>) m, content);
				EsClient.index(config.indexName, "0", m);
			}else{
				EsClient.index(config.indexName, "0", content);
			}
			data.clear();
			storeResult.setStatus(Status.success);
			System.out.println("start..4");
			return storeResult;
		} catch (Exception e) {
			e.printStackTrace();
			log.info(e.getMessage());
			storeResult.setStatus(Status.failed);
			System.out.println("start..5");
			return storeResult;
		}
	}

	@Override
	public StoreResult afterStore(ExtractedPage page) {
		return null;
	}
	
}
