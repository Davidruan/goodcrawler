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
package org.sbs.goodcrawler.extractor.selector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.select.Elements;
import org.sbs.goodcrawler.exception.ExtractException;
import org.sbs.goodcrawler.extractor.selector.action.IntegerSelectorAction;
import org.sbs.goodcrawler.extractor.selector.action.SelectorAction;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;

/**
 * @author whiteme
 * @date 2013年10月13日
 * @desc 整型抽取器，如果抽取内容不正确则返回null
 */
public class IntegerElementCssSelector extends AbstractElementCssSelector<Integer> {
	private Log log = LogFactory.getLog(IntegerElementCssSelector.class);
	private Integer content;
	private List<IntegerSelectorAction> actions = Lists.newArrayList();
	
	public IntegerElementCssSelector() {
		super();
	}

	public IntegerElementCssSelector(String name, String value, String attr,
			boolean isRequired) {
		super(name, value, attr, isRequired);
	}

	@Override
	public Integer getContent() throws ExtractException{
		Elements elements = null;
		try {
			// 如果content不为空且不是新文档，则表示是同一个document的2+次调用，不用重新计算
			if(null!=content && !newDoc){
				return content;
			}
			if(null!=document){
				elements = super.document.select(value);
				if(elements.isEmpty())
					return null;
				String temp;
				switch ($Attr) {
				case text:
					temp = CharMatcher.DIGIT.retainFrom(elements.first().text());
					break;
				default:
					temp = CharMatcher.DIGIT.retainFrom(elements.first().attr(attr));
					break;
				}
				
				if(StringUtils.isNotBlank(temp)){
					Integer integer = Integer.parseInt(temp);
					if(null!=actions && actions.size()>0){
						for(IntegerSelectorAction action:actions){
							this.content = action.doAction(integer);
						}
					}else {
						this.content = integer;
					}
					newDoc = false;
					return content;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(elements.toString());
			throw new ExtractException("信息提取错误:"+e.getMessage());
		}
		return null;
	}
	
	/**
	 * 如果content为空，且是新文档，则重新计算。
	 */
	@Override
	public Map<String, Integer> getContentMap() throws ExtractException{
		if(newDoc){
			getContent();
		}
		if(null==content)
			return null;
		Map<String, Integer> m = new HashMap<String, Integer>(1);
		m.put(name, this.content);
		return m;
	}

	public List<IntegerSelectorAction> getActions() {
		return actions;
	}

	public void setActions(List<IntegerSelectorAction> actions) {
		this.actions = actions;
	}

	public void setContent(Integer content) {
		this.content = content;
	}

	@Override
	public void addAction(SelectorAction action) {
		this.actions.add((IntegerSelectorAction) action);
	}
	
}
