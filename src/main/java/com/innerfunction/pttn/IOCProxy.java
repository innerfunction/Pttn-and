// Copyright 2016 InnerFunction Ltd.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
package com.innerfunction.pttn;

/**
 * An interface which must be implemented by configuration proxies.
 * A configuration proxy is a class for objects which are configured in place of objects of
 * another class. They are useful for providing standardized configuration APIs which are
 * consistent across platforms; or for providing simplified configuration interfaces for
 * otherwise difficult to configure objects.
 *
 * Proxies in general should support instantiation both with or without a proxied value. In the
 * second case, it is the responsibility of the proxy to instantiate the value which will be
 * returned by the unwrapValue() method.
 *
 * Proxies should declare a constructor with a single argument which can accept the value being
 * proxied. Proxies should also declare either a no-args constructor, or a constructor accepting
 * a Context object, for when there is no proxied value.
 *
 * Configuration proxy classes should be registered using the Container's registerConfigurationProxy
 * method.
 */
public interface IOCProxy {

    /**
     * Unwrap the proxied value.
     * Called at the end of the configuration cycle. Should return a fully configured instance of
     * the class being proxied.
     */
    Object unwrapValue();

}
