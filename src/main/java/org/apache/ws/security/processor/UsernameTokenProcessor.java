/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ws.security.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.UsernameTokenValidator;
import org.apache.ws.security.validate.Validator;
import org.w3c.dom.Element;

import javax.security.auth.callback.CallbackHandler;
import java.util.List;

public class UsernameTokenProcessor implements Processor {
    private static Log log = LogFactory.getLog(UsernameTokenProcessor.class.getName());
    
    private Validator validator = new UsernameTokenValidator();

    public List<WSSecurityEngineResult> handleToken(
        Element elem, Crypto crypto, Crypto decCrypto, CallbackHandler cb, 
        WSDocInfo wsDocInfo, WSSConfig wsc
    ) throws WSSecurityException {
        if (log.isDebugEnabled()) {
            log.debug("Found UsernameToken list element");
        }
        validator.setCallbackHandler(cb);
        validator.setWSSConfig(wsc);
        
        UsernameToken token = handleUsernameToken(elem, wsc);
        
        WSUsernameTokenPrincipal principal = 
            new WSUsernameTokenPrincipal(token.getName(), token.isHashed());
        principal.setNonce(token.getNonce());
        principal.setPassword(token.getPassword());
        principal.setCreatedTime(token.getCreated());
        principal.setPasswordType(token.getPasswordType());
        
        WSSecurityEngineResult result = 
            new WSSecurityEngineResult(WSConstants.UT, token, principal);
        result.put(WSSecurityEngineResult.TAG_ID, token.getID());
        wsDocInfo.addTokenElement(elem);
        wsDocInfo.addResult(result);
        return java.util.Collections.singletonList(result);
    }

    /**
     * Check the UsernameToken element. If the password type is plaintext or digested, 
     * then retrieve a password from the callback handler and authenticate the UsernameToken
     * here.
     * <p/>
     * If the password is any other yet unknown password type then delegate the password
     * validation to the callback class. Note that for unknown password types an exception
     * is thrown if WSSConfig.getHandleCustomPasswordTypes() is set to false (as it is 
     * by default). The security engine hands over all necessary data to the callback class
     * via the WSPasswordCallback object. The usage parameter of WSPasswordCallback is set to
     * <code>USERNAME_TOKEN_UNKNOWN</code>.
     *
     * @param token the DOM element that contains the UsernameToken
     * @param wssConfig The WSSConfig object from which to obtain configuration
     * @return UsernameToken the UsernameToken object that was parsed
     * @throws WSSecurityException
     */
    public UsernameToken 
    handleUsernameToken(
        Element token, 
        WSSConfig wssConfig
    ) throws WSSecurityException {
        boolean allowNamespaceQualifiedPasswordTypes = false;
        if (wssConfig != null) {
            allowNamespaceQualifiedPasswordTypes = 
                wssConfig.getAllowNamespaceQualifiedPasswordTypes();
        }
        
        //
        // Parse and validate the UsernameToken element
        //
        UsernameToken ut = new UsernameToken(token, allowNamespaceQualifiedPasswordTypes);
        Credential credential = new Credential();
        credential.setUsernametoken(ut);
        validator.validate(credential);
        
        return ut;
    }

}
