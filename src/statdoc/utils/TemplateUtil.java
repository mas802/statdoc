/*
 *   Copyright 2014-2015 Markus Schaffner
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package statdoc.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;

// import com.googlecode.htmlcompressor.compressor.HtmlCompressor;

/**
 * Provide template (and script) evaluation features.
 * 
 * @author Markus Schaffner
 * 
 */
public class TemplateUtil {

    static TemplateUtil instance;

    private String basepath;
    
    public static TemplateUtil getInstance() {
        if (instance == null) {
            throw new RuntimeException(
                    "template engine needs to be initialied before it can be accesed!");
        }
        return instance;
    }

    public TemplateUtil(String basepath) {
        instance = this;
        
        try {

            Properties p = new Properties();

            p.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
            p.setProperty("file.resource.loader.path", basepath);
            // p.setProperty("file.resource.loader.cache", "false");

            p.setProperty("velocimacro.library.autoreload", "true");
            p.setProperty("file.resource.loader.cache", "false");
            p.setProperty("file.resource.loader.modificationCheckInterval",
                    "-1");
            p.setProperty("parser.pool.size", "50");
            p.setProperty(
                    "velocimacro.permissions.allow.inline.to.replace.global",
                    "false");

            p.setProperty("runtime.log.logsystem.class",
                    "org.apache.velocity.runtime.log.NullLogSystem");

            Velocity.init(p);
            
            this.basepath = basepath;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    public String getDir() {
        return basepath;
    }

    public void evalVMtoFile(File f, String templatename,
            Map<String, Object> data) {
        /*
         * Make a context object and populate with the data. This is where the
         * Velocity engine gets the data to resolve the references (ex. $list)
         * in the template
         */

        VelocityContext context = new VelocityContext();

        for (Map.Entry<String, Object> a : data.entrySet()) {
            context.put(a.getKey(), a.getValue());
        }

        /*
        * get the Template object. This is the parsed version of your template
        * input file.
        */

        Template template = null;

        try {
            template = Velocity.getTemplate(templatename);
        } catch (ResourceNotFoundException rnfe) {
            rnfe.printStackTrace();
        } catch (ParseErrorException pee) {
            pee.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
         * work around for the 
         * "The requested operation cannot be performed 
         * on a file with a user-mapped section open."
         * Exception
         * 
         */
        if (!f.canWrite()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /*
         * Now have the template engine process your template using the data
         * placed into the context. Think of it as a 'merge' of the template and
         * the data to produce the output stream.
         */

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            template.merge(context, writer);
            writer.close();
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        } catch (ParseErrorException e) {
            e.printStackTrace();
        } catch (MethodInvocationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // velocity template evaluation
    public String evalVM(String templatename, Map<String, Object> data) {

        /*
         * Make a context object and populate with the data. This is where the
         * Velocity engine gets the data to resolve the references (ex. $list)
         * in the template
         */

        VelocityContext context = new VelocityContext();

        for (Map.Entry<String, Object> a : data.entrySet()) {
            context.put(a.getKey(), a.getValue());
        }

        /*
        * get the Template object. This is the parsed version of your template
        * input file.
        */

        Template template = null;

        try {
            template = Velocity.getTemplate(templatename);
        } catch (ResourceNotFoundException rnfe) {
            rnfe.printStackTrace();
        } catch (ParseErrorException pee) {
            pee.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
         * Now have the template engine process your template using the data
         * placed into the context. Think of it as a 'merge' of the template and
         * the data to produce the output stream.
         */

        StringWriter writer = new StringWriter();
        try {
            template.merge(context, writer);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        } catch (ParseErrorException e) {
            e.printStackTrace();
        } catch (MethodInvocationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //        HtmlCompressor compressor = new HtmlCompressor();
        //        String compressedHtml = compressor.compress(writer.toString());       

        return writer.toString();
    }

}
