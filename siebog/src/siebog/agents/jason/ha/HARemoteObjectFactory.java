/**
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements. See the NOTICE file 
 * distributed with this work for additional information regarding 
 * copyright ownership. The ASF licenses this file to you under 
 * the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may 
 * obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. 
 * 
 * See the License for the specific language governing permissions 
 * and limitations under the License.
 */

package siebog.agents.jason.ha;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import siebog.jasonee.JasonEEAgArch;
import siebog.jasonee.RemoteObjectFactory;
import siebog.jasonee.control.UserExecutionControl;
import siebog.jasonee.environment.UserEnvironment;

/**
 * 
 * @author <a href="mitrovic.dejan@gmail.com">Dejan Mitrovic</a>
 */
@Stateless
@Remote(RemoteObjectFactory.class)
public class HARemoteObjectFactory implements RemoteObjectFactory {
	private static final long serialVersionUID = 1L;

	@Override
	public JasonEEAgArch createAgArch(String className) {
		if (HAAgArch.class.getSimpleName().equals(className))
			return new HAAgArch();
		throw new IllegalArgumentException("Invalid class name: " + className);
	}

	@Override
	public UserExecutionControl createExecutionControl(String className) {
		throw new IllegalArgumentException("Invalid class name: " + className);
	}

	@Override
	public UserEnvironment createEnvironment(String className) {
		throw new IllegalArgumentException("Invalid class name: " + className);
	}
}