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
package org.sbs.goodcrawler.extractor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sbs.goodcrawler.exception.QueueException;
import org.sbs.goodcrawler.job.Page;
import org.sbs.goodcrawler.jobconf.ExtractConfig;
import org.sbs.goodcrawler.queue.PendingStore.ExtractedPage;

/**
 * @author shenbaise(shenbaise@outlook.com)
 * @date 2013-7-2
 * 默认的提取器
 */
public class DefaultExtractWorker extends ExtractWorker {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	public DefaultExtractWorker(ExtractConfig conf, Extractor extractor) {
		super(conf, extractor);
	}
	
	@Override
	public void run() {
		Page page ;
		while(!isStop()){
			try {
				while(null!=(page=pendingPages.getPage())){
					work(page);
					if(isStop())
						break;
				}
			} catch (QueueException e) {
				 log.error(e.getMessage());
			} 
		}
	}

	@Override
	public void onSuccessed(Page page) {
		// ok
		page = null;
		pendingPages.processedSuccess();
	}

	@Override
	public void onFailed(Page page) {
		pendingPages.addFailedPage(page);
	}

	@Override
	public void onIgnored(Page page) {
		pendingPages.processedIgnored();
		log.warn("忽略了一个链接："+ page.getWebURL().getURL());
	}

	@Override
	public ExtractedPage<?, ?> doExtract(Page page) {
		return extractor.onExtract(page);
	}
}
