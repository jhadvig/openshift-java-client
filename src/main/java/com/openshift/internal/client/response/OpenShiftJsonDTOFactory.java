/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package com.openshift.internal.client.response;

import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_ALIASES;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_APP_URL;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_CARTRIDGES;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_CONSUMED_GEARS;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_CREATION_TIME;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_DATA;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_DESCRIPTION;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_DISPLAY_NAME;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_DOMAIN;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_DOMAIN_ID;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_FRAMEWORK;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_GEARS;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_GEAR_PROFILE;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_GEAR_STATE;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_GIT_URL;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_HREF;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_ID;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_INITIAL_GIT_URL;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_LINKS;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_LOGIN;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_MAX_GEARS;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_METHOD;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_NAME;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_OPTIONAL_PARAMS;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_PROPERTIES;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_REL;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_REQUIRED_PARAMS;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_SCALABLE;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_SSH_URL;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_SUFFIX;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_TYPE;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_URL;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_UUID;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_VALID_OPTIONS;
import static com.openshift.internal.client.utils.IOpenShiftJsonConstants.PROPERTY_VALUE;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.client.ApplicationScale;
import com.openshift.client.GearState;
import com.openshift.client.IField;
import com.openshift.client.IGear;
import com.openshift.client.IGearProfile;
import com.openshift.client.Message;
import com.openshift.client.Messages;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftRequestException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.internal.client.Gear;
import com.openshift.internal.client.GearProfile;
import com.openshift.internal.client.utils.IOpenShiftJsonConstants;
import com.openshift.internal.client.utils.StringUtils;

/**
 * A factory for creating DTO objects.
 * 
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class OpenShiftJsonDTOFactory implements IRestResponseFactory {

	private final Logger LOGGER = LoggerFactory.getLogger(OpenShiftJsonDTOFactory.class);

	public RestResponse get(final String json) throws OpenShiftException {
		// in case the server answers with 'no-content'
		if (StringUtils.isEmpty(json)) {
			return null;
		}
		LOGGER.trace("Unmarshalling response\n{}", json);
		final ModelNode rootNode = getModelNode(json);
		final String type = rootNode.get(IOpenShiftJsonConstants.PROPERTY_TYPE).asString();
		final String status = rootNode.get(IOpenShiftJsonConstants.PROPERTY_STATUS).asString();
		final Messages messages = createMessages(rootNode.get(IOpenShiftJsonConstants.PROPERTY_MESSAGES));

		final EnumDataType dataType = EnumDataType.safeValueOf(type);
		// the response is after an error, only the messages are relevant
		

		if (dataType == null) {
			return new RestResponse(status, messages, null, null);
		}
		
		Object data = createData(dataType, messages, rootNode);
		return new RestResponse(status, messages, data, dataType);
	}

	private Object createData(EnumDataType dataType, Messages messages, ModelNode rootNode) {
		switch (dataType) {
		case user:
			return createUser(rootNode);
		case keys:
			return createKeys(rootNode);
		case key:
			return createKey(rootNode, messages);
		case links:
			return createLinks(rootNode);
		case domains:
			return createDomains(rootNode);
		case domain:
			return createDomain(rootNode, messages);
		case applications:
			return createApplications(rootNode);
		case application:
			return createApplication(rootNode, messages);
		case gear_groups:
			return createGearGroups(rootNode);
		case cartridges:
			return createCartridges(rootNode.get(PROPERTY_DATA));
		case cartridge:
			return createCartridge(rootNode, messages);
		case environment_variables:
			return createEnvironmentVariables(rootNode);
		case environment_variable:
			return createEnvironmentVariable(rootNode, messages);
			
		default:
			return null;
		}
	}
	
	/**
	 * Creates a new ResourceDTO object.
	 * 
	 * @param messagesNode
	 *            the messages node
	 * @return the list< string>
	 */
	private Messages createMessages(ModelNode messagesNode) {
		Map<IField, List<Message>> messagesByField = new LinkedHashMap<IField, List<Message>>();
		if (messagesNode.getType() == ModelType.LIST) {
			for (ModelNode messageNode : messagesNode.asList()) {
				Message message = createMessage(messageNode);
				List<Message> messages = (List<Message>) messagesByField.get(message.getField());
				if (messages == null) {
					messages = new ArrayList<Message>();
				}
				messages.add(message);
				messagesByField.put(message.getField(), messages);
			}
		}
		return new Messages(messagesByField);
	}

	
	
	private Message createMessage(ModelNode messageNode) {
		String text = getString(messageNode.get(IOpenShiftJsonConstants.PROPERTY_TEXT));
		String field = getString(messageNode.get(IOpenShiftJsonConstants.PROPERTY_FIELD));
		int exitCode = getInt(messageNode.get(IOpenShiftJsonConstants.PROPERTY_EXIT_CODE));
		String severity = getString(messageNode.get(IOpenShiftJsonConstants.PROPERTY_SEVERITY));
		return new Message(text, field, severity, exitCode);
	}

	private int getInt(ModelNode messageNode) {
		if (messageNode == null
				|| !messageNode.isDefined()) {
			return -1;
		}
		return messageNode.asInt();
	}

	private String getString(ModelNode node) {
		if (node == null
				|| !node.isDefined()) {
			return null;
		}
		return node.asString();
	}

	/**
	 * Gets the model node.
	 * 
	 * @param content
	 *            the content
	 * @return the model node
	 * @throws OpenShiftException
	 *             the open shift exception
	 */
	private ModelNode getModelNode(final String content) throws OpenShiftException {
		if (content == null) {
			throw new OpenShiftException("Could not unmarshall response: no content.");
		}
		final ModelNode node = ModelNode.fromJSONString(content);
		if (!node.isDefined()) {
			throw new OpenShiftException("Could not unmarshall response: erroneous content.");
		}

		return node;
	}

	/**
	 * Creates a new ResourceDTO object.
	 * 
	 * @param userNode
	 *            the root node
	 * @return the user resource dto
	 * @throws OpenShiftException
	 */
	private UserResourceDTO createUser(ModelNode userNode) throws OpenShiftException {
		if (userNode.has(PROPERTY_DATA)) {
			// loop inside 'data' node
			return createUser(userNode.get(PROPERTY_DATA));
		}
		final String rhlogin = getAsString(userNode, PROPERTY_LOGIN);
		final int maxGears = getAsInteger(userNode, PROPERTY_MAX_GEARS);
		final int consumedGears = getAsInteger(userNode, PROPERTY_CONSUMED_GEARS);
		final Map<String, Link> links = createLinks(userNode.get(PROPERTY_LINKS));
		return new UserResourceDTO(rhlogin, maxGears, consumedGears, links);
	}

	/**
	 * Creates a new ResourceDTO object.
	 * 
	 * @param rootNode
	 *            the root node
	 * @return the list< key resource dt o>
	 * @throws OpenShiftException
	 *             the open shift exception
	 */
	private List<KeyResourceDTO> createKeys(ModelNode rootNode) throws OpenShiftException {
		final List<KeyResourceDTO> keys = new ArrayList<KeyResourceDTO>();
		// temporarily supporting single and multiple values for 'keys' node
		if (rootNode.has(PROPERTY_DATA)) {
			for (ModelNode dataNode : rootNode.get(PROPERTY_DATA).asList()) {
				if (dataNode.getType() == ModelType.OBJECT) {
					keys.add(createKey(dataNode, null));
				}
			}
		}
		return keys;
	}

	/**
	 * Creates a new ResourceDTO object.
	 * 
	 * @param keyNode
	 *            the key node
	 * @return the key resource dto
	 * @throws OpenShiftException
	 */
	private KeyResourceDTO createKey(ModelNode keyNode, Messages messages) throws OpenShiftException {
		if (keyNode.has(PROPERTY_DATA)) {
			// loop inside 'data' node
			return createKey(keyNode.get(PROPERTY_DATA), messages);
		}
		final String name = getAsString(keyNode, IOpenShiftJsonConstants.PROPERTY_NAME);
		final String type = getAsString(keyNode, IOpenShiftJsonConstants.PROPERTY_TYPE);
		final String content = getAsString(keyNode, IOpenShiftJsonConstants.PROPERTY_CONTENT);
		final Map<String, Link> links = createLinks(keyNode.get(PROPERTY_LINKS));
		return new KeyResourceDTO(name, type, content, links, messages);
	}

	/**
	 * Creates a new set of indexed links.
	 * 
	 * @param linksNode
	 *            the root node
	 * @return the list< domain dt o>
	 * @throws OpenShiftException
	 *             the open shift exception
	 */
	private Map<String, Link> createLinks(final ModelNode linksNode) throws OpenShiftException {
		if (linksNode.has(PROPERTY_DATA)) {
			// loop inside 'data' node
			return createLinks(linksNode.get(PROPERTY_DATA));
		}
		Map<String, Link> links = new HashMap<String, Link>();
		if (linksNode.isDefined()) {
			for (ModelNode linkNode : linksNode.asList()) {
				final String linkName = linkNode.asProperty().getName();
				final ModelNode valueNode = linkNode.asProperty().getValue();
				if (valueNode.isDefined()) {
					links.put(linkName, createLink(valueNode));
				}
			}
		}
		return links;
	}

	private Link createLink(final ModelNode valueNode) {
		final String rel = getAsString(valueNode, PROPERTY_REL);
		final String href = valueNode.get(PROPERTY_HREF).asString();
		final String method = valueNode.get(PROPERTY_METHOD).asString();
		final List<LinkParameter> requiredParams = 
				createLinkParameters(valueNode.get(PROPERTY_REQUIRED_PARAMS));
		final List<LinkParameter> optionalParams = 
				createLinkParameters(valueNode.get(PROPERTY_OPTIONAL_PARAMS));
		return new Link(rel, href, method, requiredParams, optionalParams);
	}

	/**
	 * Creates a new DTO object.
	 * 
	 * @param rootNode
	 *            the root node
	 * @return the list< domain dt o>
	 * @throws OpenShiftException
	 *             the open shift exception
	 */
	private List<DomainResourceDTO> createDomains(final ModelNode rootNode) throws OpenShiftException {
		final List<DomainResourceDTO> domains = new ArrayList<DomainResourceDTO>();
		// temporarily supporting absence of 'data' node in the 'domain'
		// response message
		// FIXME: simplify once openshift response is fixed
		if (rootNode.has(PROPERTY_DATA)) {
			for (ModelNode dataNode : rootNode.get(PROPERTY_DATA).asList()) {
				if (dataNode.getType() == ModelType.OBJECT) {
					domains.add(createDomain(dataNode, null));
				} else {
					throw new OpenShiftException("Unexpected node type: {0}", dataNode.getType());
				}
			}
		} else {
			final ModelNode domainNode = rootNode.get(PROPERTY_DOMAIN);
			if (domainNode.isDefined()
					&& domainNode.getType() == ModelType.OBJECT) {
				domains.add(createDomain(domainNode, null));
			} else {
				throw new OpenShiftException("Unexpected node type: {0}", domainNode.getType());
			}
		}

		return domains;
	}

	/**
	 * Creates a new DTO object.
	 * 
	 * @param domainNode
	 *            the domain node
	 * @return the domain dto
	 * @throws OpenShiftException
	 */
	private DomainResourceDTO createDomain(final ModelNode domainNode, Messages messages)
			throws OpenShiftException {
		if (domainNode.has(PROPERTY_DATA)) {
			// recurse into "data" node
			return createDomain(domainNode.get(PROPERTY_DATA), messages);
		}
		final String namespace = getAsString(domainNode, PROPERTY_ID);
		final String suffix = getAsString(domainNode, PROPERTY_SUFFIX);
		final Map<String, Link> links = createLinks(domainNode.get(PROPERTY_LINKS));
		return new DomainResourceDTO(namespace, suffix, links, messages);
	}

	/**
	 * Creates a new DTO object.
	 * 
	 * @param rootNode
	 *            the domain node
	 * @return the list< application dt o>
	 * @throws OpenShiftException
	 */
	private List<ApplicationResourceDTO> createApplications(final ModelNode rootNode)
			throws OpenShiftException {
		final List<ApplicationResourceDTO> applicationDTOs = new ArrayList<ApplicationResourceDTO>();
		if (rootNode.has(PROPERTY_DATA)) {
			for (ModelNode applicationNode : rootNode.get(PROPERTY_DATA).asList()) {
				applicationDTOs.add(createApplication(applicationNode, null));
			}
		}
		return applicationDTOs;
	}

	/**
	 * Creates a new DTO object.
	 * 
	 * @param appNode
	 *            the app node
	 * @return the application dto
	 * @throws OpenShiftException
	 */
	private ApplicationResourceDTO createApplication(ModelNode appNode, Messages messages)
			throws OpenShiftException {
		if (appNode.has(PROPERTY_DATA)) {
			// recurse into 'data' node
			return createApplication(appNode.get(PROPERTY_DATA), messages);
		}
		final String framework = getAsString(appNode, PROPERTY_FRAMEWORK);
		final String creationTime = getAsString(appNode, PROPERTY_CREATION_TIME);
		final String name = getAsString(appNode, PROPERTY_NAME);
		final String uuid = getAsString(appNode, PROPERTY_UUID);
		final ApplicationScale scalable = ApplicationScale.safeValueOf(getAsString(appNode, PROPERTY_SCALABLE));
		final IGearProfile gearProfile = createGearProfile(appNode);
		final String applicationUrl = getAsString(appNode, PROPERTY_APP_URL);
		final String sshUrl = getAsString(appNode, PROPERTY_SSH_URL);
		final String gitUrl = getAsString(appNode, PROPERTY_GIT_URL);
		final String initialGitUrl = getAsString(appNode, PROPERTY_INITIAL_GIT_URL);
		final String domainId = getAsString(appNode, PROPERTY_DOMAIN_ID);
		final Map<String, Link> links = createLinks(appNode.get(PROPERTY_LINKS));
		final List<String> aliases = createAliases(appNode.get(PROPERTY_ALIASES));
		final Map<String, CartridgeResourceDTO> cartridges = createCartridges(appNode.get(PROPERTY_CARTRIDGES));
		
		return new ApplicationResourceDTO(
				framework, 
				domainId, 
				creationTime, 
				name, 
				gearProfile, 
				scalable, 
				uuid, 
				applicationUrl, 
				sshUrl,
				gitUrl, 
				initialGitUrl,
				aliases, 
				cartridges, 
				links, 
				messages);
	}

	private GearProfile createGearProfile(ModelNode appNode) {
		String gearProfileName = getAsString(appNode, PROPERTY_GEAR_PROFILE);
		if (gearProfileName == null) {
			return null;
		}
		return new GearProfile(gearProfileName);
	}

	private Collection<GearGroupResourceDTO> createGearGroups(ModelNode dataNode) {
		Collection<GearGroupResourceDTO> gearGroupDTOs = new ArrayList<GearGroupResourceDTO>();
		for(ModelNode gearGroupNode : dataNode.get(PROPERTY_DATA).asList()) {
			gearGroupDTOs.add(createGearGroupResourceDTO(gearGroupNode));
		}
		
		return gearGroupDTOs;
	}

	private GearGroupResourceDTO createGearGroupResourceDTO(ModelNode gearGroupNode) {
		final String uuid = getAsString(gearGroupNode, PROPERTY_UUID);
		final String name = getAsString(gearGroupNode, PROPERTY_NAME);
		final Collection<GearResourceDTO> gears = createGears(gearGroupNode.get(PROPERTY_GEARS));
		final Map<String, CartridgeResourceDTO> cartridges = createCartridges(gearGroupNode.get(PROPERTY_CARTRIDGES));
		return new GearGroupResourceDTO(uuid, name, gears, cartridges);
	}
	
	private Collection<GearResourceDTO> createGears(ModelNode gearsNode) {
		List<GearResourceDTO> gears = new ArrayList<GearResourceDTO>();
		for (ModelNode gearNode : gearsNode.asList()) {
			gears.add(
					new GearResourceDTO(
							getAsString(gearNode, PROPERTY_ID),
							getAsString(gearNode, PROPERTY_GEAR_STATE),
							getAsString(gearNode, PROPERTY_SSH_URL)));
		}
		return gears;
	}

	/**
	 * Creates a new CartridgeResourceDTO for a given root node.
	 * 
	 * @param cartridgesNode
	 *            the root node
	 * @return the list< cartridge resource dto>
	 * @throws OpenShiftException
	 */
	private Map<String, CartridgeResourceDTO> createCartridges(ModelNode cartridgesNode) throws OpenShiftException {
		final Map<String, CartridgeResourceDTO> cartridgesByName = new LinkedHashMap<String, CartridgeResourceDTO>();
		if (cartridgesNode.isDefined()
				&& cartridgesNode.getType() == ModelType.LIST) {
			for (ModelNode cartridgeNode : cartridgesNode.asList()) {
				CartridgeResourceDTO cartridgeResourceDTO = createCartridge(cartridgeNode, null);
				cartridgesByName.put(cartridgeResourceDTO.getName(), cartridgeResourceDTO);
			}
		}
		return cartridgesByName;
	}

	/**
	 * Creates a new CartridgeResourceDTO object for a given cartridge node and messages.
	 * 
	 * @param cartridgeNode
	 *            the cartridge node
	 * @return the cartridge resource dto
	 * @throws OpenShiftException
	 */
	private CartridgeResourceDTO createCartridge(ModelNode cartridgeNode, Messages messages)
			throws OpenShiftException {
		if (cartridgeNode.has(PROPERTY_DATA)) {
			// recurse into 'data' node
			return createCartridge(cartridgeNode.get(PROPERTY_DATA), messages);
		}
		
		final String name = getAsString(cartridgeNode, PROPERTY_NAME);
		final String displayName = getAsString(cartridgeNode, PROPERTY_DISPLAY_NAME);
		final String description = getAsString(cartridgeNode, PROPERTY_DESCRIPTION);
		final String type = getAsString(cartridgeNode, PROPERTY_TYPE);
		final URL url = createUrl(getAsString(cartridgeNode, PROPERTY_URL), name);
		final CartridgeResourceProperties properties = createProperties(cartridgeNode.get(PROPERTY_PROPERTIES));
		final Map<String, Link> links = createLinks(cartridgeNode.get(PROPERTY_LINKS));
		return new CartridgeResourceDTO(name, displayName, description, type, url, properties, links, messages);
	}

	private URL createUrl(String url, String name) {
		try {
			if (url == null) {
				return null;
			}
			return new URL(url);
		} catch (MalformedURLException e) {
			LOGGER.warn("Url {} in server response for cartridge {} is not a valid URL.", url, name);
			return null;
		}
	}
	
	/**
	 * Creates a new ResourceDTO object.
	 * 
	 * @param aliasNodeList
	 *            the alias node list
	 * @return the list< string>
	 */
	private List<String> createAliases(ModelNode aliasNodesList) {
		final List<String> aliases = new ArrayList<String>();
		switch (aliasNodesList.getType()) {
		case OBJECT:
		case LIST:
			for (ModelNode aliasNode : aliasNodesList.asList()) {
				aliases.add(aliasNode.asString());
			}
			break;
		default:
			aliases.add(aliasNodesList.asString());
		}
		return aliases;
	}

	/**
	 * Creates a new DTO object.
	 * 
	 * @param linkParamNodes
	 *            the link param nodes
	 * @return the list< link param>
	 * @throws OpenShiftRequestException
	 */
	private List<LinkParameter> createLinkParameters(ModelNode linkParamNodes)
			throws OpenShiftRequestException {
		List<LinkParameter> linkParams = new ArrayList<LinkParameter>();
		if (linkParamNodes.isDefined()) {
			for (ModelNode linkParamNode : linkParamNodes.asList()) {
				linkParams.add(createLinkParameter(linkParamNode));
			}
		}
		return linkParams;
	}

	/**
	 * Creates a new link parameter for the given link parameter node.
	 * 
	 * @param linkParamNode
	 *            the model node that contains the link parameters
	 * @return the link parameter
	 * @throws OpenShiftRequestException
	 */
	private LinkParameter createLinkParameter(ModelNode linkParamNode) throws OpenShiftRequestException {
		final String description = linkParamNode.get(IOpenShiftJsonConstants.PROPERTY_DESCRIPTION).asString();
		final String type = linkParamNode.get(IOpenShiftJsonConstants.PROPERTY_TYPE).asString();
		final String defaultValue = linkParamNode.get(IOpenShiftJsonConstants.PROPERTY_DEFAULT_VALUE).asString();
		final String name = linkParamNode.get(IOpenShiftJsonConstants.PROPERTY_NAME).asString();
		return new LinkParameter(name, type, defaultValue, description, createValidOptions(linkParamNode));
	}

	/**
	 * Gets the valid options.
	 * 
	 * @param linkParamNode
	 *            the link param node
	 * @return the valid options
	 */
	private List<String> createValidOptions(ModelNode linkParamNode) {
		final List<String> validOptions = new ArrayList<String>();
		final ModelNode validOptionsNode = linkParamNode.get(PROPERTY_VALID_OPTIONS);
		if (validOptionsNode.isDefined()) {
			switch (validOptionsNode.getType()) {
			case STRING: // if there's only one value, it is not serialized as a
							// list, but just a string
				validOptions.add(validOptionsNode.asString());
				break;
			case LIST:
				for (ModelNode validOptionNode : validOptionsNode.asList()) {
					validOptions.add(validOptionNode.asString());
				}
				break;
			default:
				break;
			}
		}
		return validOptions;
	}

	/**
	 * Returns the property identified by the given name in the given model
	 * node, or null if the named property is undefined.
	 * 
	 * @param node
	 *            the model node
	 * @param propertyName
	 *            the name of the property
	 * @return the property as a String
	 */
	private String getAsString(final ModelNode node, String propertyName) {
		final ModelNode propertyNode = node.get(propertyName);
		return propertyNode.isDefined() ? propertyNode.asString() : null;
	}
	
	/**
	 * Creates ResourceProperties for a given propertiesNode
	 * <p>
	 * ex.
	 * 
	 * <pre>
	 * "properties":[
	 *       {
	 *          "name":"connection_url",
	 *          "type":"cart_data",
	 *          "description":"Application metrics URL",
	 *          "value":"https://eap6-foobarz.rhcloud.com/metrics/"
	 *       },
	 * </pre>
	 * 
	 * @param propertiesNode
	 * @return
	 */
	private CartridgeResourceProperties createProperties(ModelNode propertiesNode) {
		if (propertiesNode == null
				|| !propertiesNode.isDefined()) {
			return null;
		}
		
		CartridgeResourceProperties properties = new CartridgeResourceProperties();
		for(ModelNode propertyNode : propertiesNode.asList()) {
			CartridgeResourceProperty property = createProperty(propertyNode);
			String name = property.getName();
			if (StringUtils.isEmpty(name)) {
				continue;
			}
			properties.add(name, property);
		}
		return properties;
	}

	private CartridgeResourceProperty createProperty(ModelNode propertyNode) {
		String name = getAsString(propertyNode, IOpenShiftJsonConstants.PROPERTY_NAME);
		String description = getAsString(propertyNode, IOpenShiftJsonConstants.PROPERTY_DESCRIPTION);
		String type = getAsString(propertyNode, IOpenShiftJsonConstants.PROPERTY_TYPE);
		String value = getAsString(propertyNode, IOpenShiftJsonConstants.PROPERTY_VALUE);
		return new CartridgeResourceProperty(name, type, description, value);
	}
	
	/**
	 * Returns the property identified by the given name in the given model node, or null if the named property is
	 * undefined.
	 * 
	 * @param node
	 *            the model node
	 * @param propertyName
	 *            the name of the property
	 * @return the property as a String
	 */
	@SuppressWarnings("unused")
	private Boolean getAsBoolean(final ModelNode node, String propertyName) {
		final ModelNode propertyNode = node.get(propertyName);
		return propertyNode.isDefined() ? propertyNode.asBoolean() : Boolean.FALSE;
	}
	
	/**
	 * Returns the property identified by the given name in the given model node, or null if the named property is
	 * undefined.
	 * 
	 * @param node
	 *            the model node
	 * @param propertyName
	 *            the name of the property
	 * @return the property as an Integer
	 */
	private int getAsInteger(final ModelNode node, String propertyName) {
		final ModelNode propertyNode = node.get(propertyName);
		return propertyNode.isDefined() ? propertyNode.asInt() : 0;
	}
	
	private List<EnvironmentVariableResourceDTO> createEnvironmentVariables(ModelNode rootNode)
			throws OpenShiftException {
		final List<EnvironmentVariableResourceDTO> environmentVariables = new ArrayList<EnvironmentVariableResourceDTO>();
		if (rootNode.has(PROPERTY_DATA)) {
			for (ModelNode environmentVariableNode : rootNode.get(PROPERTY_DATA).asList()) {
				environmentVariables.add(createEnvironmentVariable(environmentVariableNode, null));
			}
		}
		return environmentVariables;
	}

	private EnvironmentVariableResourceDTO createEnvironmentVariable(ModelNode environmentVariableNode,
			Messages messages)
			throws OpenShiftException {
		if (environmentVariableNode.has(PROPERTY_DATA)) {
			// recurse into 'data' node
			return createEnvironmentVariable(environmentVariableNode.get(PROPERTY_DATA), messages);
		}
		final String name = getAsString(environmentVariableNode, PROPERTY_NAME);
		final String value = getAsString(environmentVariableNode, PROPERTY_VALUE);
		final Map<String, Link> links = createLinks(environmentVariableNode.get(PROPERTY_LINKS));
		return new EnvironmentVariableResourceDTO(name, value, links, messages);
	}
}
