/**
 * ########################  SHENBAISE'S WORK  ##########################
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
package org.sbs.goodcrawler.jobconf;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.sbs.goodcrawler.conf.Configuration;

/**
 * @author whiteme
 * @date 2013年8月3日
 * @desc 存储配置对象，默认使用es
 */
public class StoreConfig extends Configuration {
	private Log log = LogFactory.getLog(this.getClass());
	private String type = "default";
	private int threadNum = 2;
	private String pluginClass = null;
	
	public StoreConfig() {
	}
	
	public StoreConfig loadConfig(Document confDoc){
		Document doc = confDoc;
		jobName = doc.select("job").attr("name");
		indexName = doc.select("job").attr("indexName");
		Elements e = doc.select("store");
		this.type = e.select("type").text();
		if(StringUtils.isNotBlank(e.select("threadNum").text())){
			this.threadNum = Integer.parseInt(e.select("threadNum").text());
		}
		String className = e.select("plugin").text();
		if(StringUtils.isNotBlank(className)){
			this.pluginClass = className;
		}
		return this;
	}
	
	public Log getLog() {
		return log;
	}

	public void setLog(Log log) {
		this.log = log;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getThreadNum() {
		return threadNum;
	}

	public void setThreadNum(int threadNum) {
		this.threadNum = threadNum;
	}
	
	public String getPluginClass() {
		return pluginClass;
	}

	public void setPluginClass(String pluginClass) {
		this.pluginClass = pluginClass;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StoreConfig [log=").append(log).append(", type=")
				.append(type).append(", threadNum=").append(threadNum)
				.append(", pluginClass=").append(pluginClass).append("]");
		return builder.toString();
	}

	public static void main(String[] args) {
		StoreConfig extractConfig = new StoreConfig();
		Document document;
		try {
			document = Jsoup.parse(new File("conf/youku_conf.xml"), "utf-8");
			System.out.println(extractConfig.loadConfig(document).toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
