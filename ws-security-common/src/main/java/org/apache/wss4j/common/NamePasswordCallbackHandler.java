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
package org.apache.wss4j.common;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class NamePasswordCallbackHandler implements CallbackHandler {  

    private static final org.slf4j.Logger LOG = 
        org.slf4j.LoggerFactory.getLogger(NamePasswordCallbackHandler.class);
    
    private static final String PASSWORD_CALLBACK_NAME = "setObject";
    private static final Class<?>[] PASSWORD_CALLBACK_TYPES = 
        new Class<?>[]{Object.class, char[].class, String.class};
    
    private String username;  
    private String password;  
    
    private String passwordCallbackName;
    
    public NamePasswordCallbackHandler(String username, String password) {  
        this(username, password, null);  
    }  
     
    public NamePasswordCallbackHandler(String username, String password, String passwordCallbackName) {  
        this.username = username;  
        this.password = password;
        this.passwordCallbackName = passwordCallbackName;
    }  

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {  
        for (int i = 0; i < callbacks.length; i++) {  
            Callback callback = callbacks[i];
            if (handleCallback(callback)) {
                continue;
            } else if (callback instanceof NameCallback) {  
                ((NameCallback) callback).setName(username);  
            } else if (callback instanceof PasswordCallback) {  
                PasswordCallback pwCallback = (PasswordCallback) callback;  
                pwCallback.setPassword(password.toCharArray());
            } else if (!invokePasswordCallback(callback)) {
                LOG.error("Unsupported callback type " + callbacks[i].getClass().getName());
                throw new UnsupportedCallbackException(callbacks[i], "Unsupported callback type " + callbacks[i].getClass().getName());  
            }  
        }  
    }      
    
    protected boolean handleCallback(Callback callback) {
        return false;
    }
    
    /*
     * This method is called from the handle(Callback[]) method when the specified callback 
     * did not match any of the known callback classes. It looks for the callback method 
     * having the specified method name with one of the supported parameter types.
     * If found, it invokes the callback method on the object and returns true. 
     * If not, it returns false.
     */
    private boolean invokePasswordCallback(Callback callback) {
        String cbname = passwordCallbackName == null
                        ? PASSWORD_CALLBACK_NAME : passwordCallbackName;
        for (Class<?> arg : PASSWORD_CALLBACK_TYPES) {
            try {
                Method method = callback.getClass().getMethod(cbname, arg);
                method.invoke(callback, arg == String.class ? password : password.toCharArray());
                return true;
            } catch (Exception e) {
                // ignore and continue
                LOG.warn(e.toString());
            }
        }
        return false;
    }
 
}

