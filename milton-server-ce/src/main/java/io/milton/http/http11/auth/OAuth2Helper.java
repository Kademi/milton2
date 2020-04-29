/*
 * Copyright 2015 McEvoy Software Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.milton.http.http11.auth;

import io.milton.common.Utils;
import io.milton.http.OAuth2TokenResponse;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.resource.OAuth2Provider;
import io.milton.resource.OAuth2Resource.OAuth2ProfileDetails;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Lee YOU
 */
public class OAuth2Helper {

	private static final Logger log = LoggerFactory.getLogger(OAuth2Helper.class);

	public static URL getOAuth2URL(OAuth2Provider provider, String returnUrl) {
		return getOAuth2URL(provider, returnUrl, null);
	}

	public static URL getOAuth2URL(OAuth2Provider provider, String returnUrl, Map<String, String> extraStateParameters) {
		log.trace("getOAuth2URL {}", provider);

		String oAuth2Location = provider.getAuthLocation();
		String oAuth2ClientId = provider.getClientId();
		String scopes = Utils.toCsv(provider.getPermissionScopes(), false);
		try {
			String state = toState(provider.getProviderId(), returnUrl, extraStateParameters);
			OAuthClientRequest oAuthRequest = OAuthClientRequest
					.authorizationLocation(oAuth2Location)
					.setClientId(oAuth2ClientId)
					.setResponseType("code")
					.setScope(scopes)
					.setState(state)
					.setRedirectURI(provider.getRedirectURI())
					.buildQueryMessage();

			return new URL(oAuthRequest.getLocationUri());
		} catch (OAuthSystemException oAuthSystemException) {
			throw new RuntimeException(oAuthSystemException);
		} catch (MalformedURLException malformedURLException) {
			throw new RuntimeException(malformedURLException);
		}
	}

	public static String toState(String providerId, String returnUrl) {
		return toState(providerId, returnUrl, null);
	}

	public static String toState(String providerId, String returnUrl, Map<String, String> extraStateParameters) {
		StringBuilder sb = new StringBuilder(providerId);
		if (returnUrl != null) {
			sb.append("||");
			sb.append(returnUrl);
		}

		if (extraStateParameters != null && !extraStateParameters.isEmpty()) {
			final List<String> extraParams = new ArrayList();
			extraStateParameters.forEach((String k, String v) -> {
				extraParams.add(StringUtils.trimToEmpty(k) + "=" + StringUtils.trimToEmpty(v));
			});

			if (!extraParams.isEmpty()) {
				sb.append(StringUtils.join(extraParams, "||"));
			}
		}

		byte[] arr = Base64.encode(sb.toString().getBytes());
		String encoded = new String(arr);
		return encoded;
	}

	public static oAuth2State parseState(String encoded) {
		String decoded = new String(Base64.decode(encoded));
		String[] paramParts = StringUtils.split(decoded, "||");

		String p = null;
		String r = null;
		Map<String, String> extraParams = new HashMap();

		if (paramParts != null) {
			// Parse providerId
			if (paramParts.length >= 1) {
				p = paramParts[0];
			}

			// Parse returnUrl
			if (paramParts.length >= 2) {
				r = paramParts[1];
			}

			// Parse extraParams
			if (paramParts.length >= 3) {
				for (int ppi = 2; ppi < paramParts.length; ppi++) {
					String extraParam = paramParts[ppi];

					String k = null;
					String v = null;

					String[] extraParamPart = StringUtils.split(extraParam, "=");
					if (extraParamPart != null) {
						if (extraParamPart.length >= 1) {
							k = StringUtils.trimToNull(extraParamPart[0]);
						}
						if (extraParamPart.length >= 2) {
							v = StringUtils.trimToEmpty(extraParamPart[1]);
						}
					}

					if (StringUtils.isNotBlank(k)) {
						extraParams.put(k, v);
					}
				}
			}
		}

		return new oAuth2State(p, r, extraParams);
	}

	private final NonceProvider nonceProvider;

	public OAuth2Helper(NonceProvider nonceProvider) {
		this.nonceProvider = nonceProvider;
	}

	// Sept 2, After Got The Authorization Code(a Access Permission), then Granting the Access Token.
	public OAuthAccessTokenResponse obtainAuth2Token(OAuth2Provider provider, String accessCode) throws OAuthSystemException, OAuthProblemException {
		log.trace("obtainAuth2Token code={}, provider={}", accessCode, provider);

		String oAuth2ClientId = provider.getClientId();
		String oAuth2TokenLocation = provider.getTokenLocation();
		String oAuth2ClientSecret = provider.getClientSecret();
		String oAuth2RedirectURI = provider.getRedirectURI();

		OAuthClientRequest oAuthRequest = OAuthClientRequest
				.tokenLocation(oAuth2TokenLocation)
				.setGrantType(GrantType.AUTHORIZATION_CODE)
				.setRedirectURI(oAuth2RedirectURI)
				.setCode(accessCode)
				.setClientId(oAuth2ClientId)
				.setClientSecret(oAuth2ClientSecret)
				.buildBodyMessage();

		OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

		// This works for facebook
		OAuthAccessTokenResponse oAuth2Response2 = oAuthClient.accessToken(oAuthRequest, OAuth2TokenResponse.class);
		//return oAuth2Response;

		// This might work for google
		OAuthJSONAccessTokenResponse o;
		//OAuthAccessTokenResponse oAuth2Response2 = oAuthClient.accessToken(oAuthRequest, OAuth2TokenResponse.class);
		return oAuth2Response2;

	}

	// Sept 3, GET the profile of the user.
	public OAuthResourceResponse getOAuth2Profile(OAuthAccessTokenResponse oAuth2Response, OAuth2Provider provider)
			throws OAuthSystemException, OAuthProblemException {

		log.trace("getOAuth2Profile start {}", oAuth2Response);

		String accessToken = oAuth2Response.getAccessToken();
		String userProfileLocation = provider.getProfileLocation();

		if (StringUtils.isNotBlank(userProfileLocation)) {
                    OAuthBearerClientRequest builder = new OAuthBearerClientRequest(userProfileLocation)
					.setAccessToken(accessToken);

			OAuthClientRequest bearerClientRequest;

			if (null == provider.getOAuth2AccessTokenType()) {
				bearerClientRequest = builder.buildQueryMessage();
			} else {
				switch (provider.getOAuth2AccessTokenType()) {
					case REQUEST_PARAM:
						bearerClientRequest = builder.buildQueryMessage();
						break;
					case BEARER:
						bearerClientRequest = builder.buildHeaderMessage();
						break;
					case BODY:
						bearerClientRequest = builder.buildBodyMessage();
						break;
					default:
						bearerClientRequest = builder.buildQueryMessage();
						break;
				}
			}

			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

			return oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);
		}

		return null;
	}

	public OAuth2ProfileDetails getOAuth2UserInfo(Request request, OAuthResourceResponse resourceResponse, OAuthAccessTokenResponse tokenResponse, OAuth2Provider prov, String oAuth2Code, String returnUrl) throws BadRequestException {
		log.trace(" getOAuth2UserId start..." + resourceResponse);

		Map responseMap = null;
		if (resourceResponse != null) {
			String resourceResponseBody = resourceResponse.getBody();
			log.trace(" OAuthResourceResponse, body{}" + resourceResponseBody);

			request.getAttributes().put(OAuth2AuthenticationHandler.REQ_ATT_OAUTH_JSON, resourceResponseBody);

			responseMap = JSONUtils.parseJSON(resourceResponseBody);

			String userID = (String) responseMap.get("id");
			String userName = (String) responseMap.get("username");
			String message = (String) responseMap.get("message");
			Integer status = -1;
			Object errCode = responseMap.get("status");
			if (errCode instanceof Integer) {
				status = (Integer) errCode;
			} else if (errCode instanceof String) {
				status = Integer.valueOf((String) errCode);
			}

			if (status >= 400) {
				throw new BadRequestException(message);
			}

			if (log.isTraceEnabled()) {
				log.trace(" userID{}" + userID);
				log.trace(" userName{}" + userName);
			}
		}

		OAuth2ProfileDetails user = new OAuth2ProfileDetails();
		user.setCode(oAuth2Code);
		user.setAccessToken(tokenResponse.getAccessToken());
		user.setRefreshToken(tokenResponse.getRefreshToken());
		user.setExpiresIn(tokenResponse.getExpiresIn());
		user.setDetails(responseMap);
		user.setReturnUrl(returnUrl);

		if (prov != null) {
			user.setTokenLocation(prov.getTokenLocation());
			user.setProviderId(prov.getProviderId());
		}

		if (log.isTraceEnabled()) {
			log.trace(" oAuth2Code{}" + oAuth2Code);
			log.trace(" AccessToken{}" + user.getAccessToken());
			log.trace("\n\n");
		}

		return user;
	}
}
