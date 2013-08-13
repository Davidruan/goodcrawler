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
package org.sbs.goodcrawler.fetcher;

import org.sbs.goodcrawler.conf.jobconf.FetchConfig;

/**
 * @author whiteme
 * @date 2013年7月29日
 * @desc 
 */
public class FetcherInstance {
	private static FetchConfig conf = new FetchConfig();
	{
		conf.setAgent("ipad");
		conf.setSocketTimeoutMilliseconds(15000);
		conf.setConnectionTimeout(5000);
		conf.setMaxTotalConnections(10);
		conf.setHttps(true);
	}
	
	private static PageFetcher fetcher = null;
	
	public static PageFetcher getFetcher(){
		if(null==fetcher){
			fetcher = new PageFetcher(conf);
		}
		return fetcher;
	}
}
