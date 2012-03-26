/*
 * Copyright 2011 <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ocpsoft.rewrite.servlet.config;

import org.ocpsoft.common.util.Assert;

import org.ocpsoft.rewrite.bind.Evaluation;
import org.ocpsoft.rewrite.bind.ParameterizedPattern;
import org.ocpsoft.rewrite.bind.RegexOperationParameterBuilder;
import org.ocpsoft.rewrite.config.Operation;
import org.ocpsoft.rewrite.context.EvaluationContext;
import org.ocpsoft.rewrite.param.OperationParameterBuilder;
import org.ocpsoft.rewrite.param.Parameter;
import org.ocpsoft.rewrite.param.ParameterizedOperation;
import org.ocpsoft.rewrite.servlet.http.event.HttpInboundServletRewrite;
import org.ocpsoft.rewrite.servlet.http.event.HttpOutboundServletRewrite;
import org.ocpsoft.rewrite.servlet.http.event.HttpServletRewrite;
import org.ocpsoft.rewrite.bind.Binding;

/**
 * Responsible for substituting inbound/outbound URLs with a replacement. For {@link org.ocpsoft.rewrite.event.InboundRewrite} events, this
 * {@link Operation} calls {@link HttpInboundServletRewrite#forward(String)}, and for {@link org.ocpsoft.rewrite.event.OutboundRewrite} events,
 * this method calls {@link HttpOutboundServletRewrite#setOutboundURL(String)}
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class Substitute extends HttpOperation implements
ParameterizedOperation<OperationParameterBuilder<RegexOperationParameterBuilder, String>, String>
{
   private final ParameterizedPattern location;

   private Substitute(final String location)
   {
      Assert.notNull(location, "Location must not be null.");
      this.location = new ParameterizedPattern("[^/]+", location);

      for (Parameter<String> parameter : this.location.getParameters().values()) {
         parameter.bindsTo(Evaluation.property(parameter.getName()));
      }
   }

   /**
    * Substitute the current URL with the given location.
    * <p>
    * The given location may be parameterized using the following format:
    * <p>
    * <code>
    *    /example/{param} <br>
    *    /example/{value}/sub/{value2} <br>
    *    ... and so on
    * </code>
    * <p>
    * Parameters may be bound. By default, matching parameter values are extracted from bindings in the
    * {@link org.ocpsoft.rewrite.context.EvaluationContext}.
    * <p>
    * See also {@link #where(String)}
    */
   public static Substitute with(final String location)
   {
      return new Substitute(location);
   }

   @Override
   public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
   {
      if (event instanceof HttpInboundServletRewrite)
      {
         String target = location.build(event, context);
         ((HttpInboundServletRewrite) event).forward(target);
      }
      else if (event instanceof HttpOutboundServletRewrite)
      {
         String target = location.build(event, context);
         if (((HttpOutboundServletRewrite) event).getOutboundURL().startsWith(event.getContextPath())
                  && target.startsWith("/")
                  && !target.startsWith(event.getContextPath()))
         {
            target = event.getContextPath() + target;
         }
         ((HttpOutboundServletRewrite) event).setOutboundURL(target);
      }
   }

   @Override
   public RegexOperationParameterBuilder where(final String param)
   {
      return new RegexOperationParameterBuilder(this, location.getParameter(param));
   }

   @Override
   public RegexOperationParameterBuilder where(final String param, final String pattern)
   {
      return where(param).matches(pattern);
   }

   @Override
   public RegexOperationParameterBuilder where(final String param, final String pattern,
            final Binding binding)
   {
      return where(param, pattern).bindsTo(binding);
   }

   @Override
   public RegexOperationParameterBuilder where(final String param, final Binding binding)
   {
      return where(param).bindsTo(binding);
   }
}