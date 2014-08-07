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

package siebog.server.xjaf.cnm;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.NamingException;
import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import siebog.server.xjaf.utils.config.RelayInfo;
import siebog.server.xjaf.utils.config.XjafCluster;

/**
 * Default connection manager implementation.
 * 
 * @author <a href="mailto:mitrovic.dejan@gmail.com">Dejan Mitrovic</a>
 */
@Stateless
@Remote(ConnectionManagerI.class)
public class ConnectionManager implements ConnectionManagerI
{
	private static final Logger logger = Logger.getLogger(ConnectionManager.class.getName());
	private JChannel channel;
	
	@PostConstruct
	public void postConstruct() throws NamingException
	{
		RelayInfo relay = XjafCluster.get().getRelay();
		if (relay == null)
			logger.info("Relay not specified, support for remote clusters disabled");
		else
		{
			try
			{
				channel = new JChannel(XjafCluster.class.getResource("/site-config.xml"));
				channel.connect("xjaf-global-cluster");
				channel.setReceiver(new ReceiverAdapter() {
					
				});
				if (logger.isLoggable(Level.INFO))
					logger.info("ConnectionManager initialized");
			} catch (Exception ex)
			{
				logger.log(Level.SEVERE, "Unable to create channel", ex);
			}
		}
	}
}