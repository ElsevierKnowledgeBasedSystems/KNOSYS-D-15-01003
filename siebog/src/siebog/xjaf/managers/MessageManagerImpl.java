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

package siebog.xjaf.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.Form;
import siebog.core.Global;
import siebog.xjaf.core.AID;
import siebog.xjaf.fipa.ACLContent;
import siebog.xjaf.fipa.ACLMessage;
import siebog.xjaf.fipa.Performative;

/**
 * Default message manager implementation.
 * 
 * @author <a href="mitrovic.dejan@gmail.com">Dejan Mitrovic</a>
 * @author <a href="rade.milovanovic@hotmail.com">Rade Milovanovic</a>
 */
@Stateless
@Remote(MessageManager.class)
@Path("/messages")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@LocalBean
public class MessageManagerImpl implements MessageManager {
	private static final Logger logger = Logger.getLogger(MessageManagerImpl.class.getName());
	@EJB
	private AgentManagerImpl agentManager;

	@GET
	@Path("/")
	public List<String> getPerformatives() {
		final Performative[] arr = Performative.values();
		List<String> list = new ArrayList<>(arr.length);
		for (Performative p : arr)
			list.add(p.toString());
		return list;
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Override
	public void post(@Form ACLMessage msg) {
		for (AID aid : msg.getReceivers()) {
			if (aid == null)
				throw new IllegalArgumentException("Receiver AID cannot be null.");
			try {
				RunningAgent rec = agentManager.getRunningAgent(aid);
				rec.deliverMessage(msg);
			} catch (IllegalArgumentException ex) {
				logger.info(ex.getMessage());
			}
		}
	}

	@Override
	public void post(AID sender, AID receiver, Performative performative, String content) {
		ACLMessage msg = new ACLMessage(performative);
		msg.setSender(sender);
		msg.addReceiver(receiver);
		msg.setContent(new ACLContent(content));
		post(msg);
	}

	@Override
	public String ping() {
		return "Pong from " + Global.getNodeName();
	}

}