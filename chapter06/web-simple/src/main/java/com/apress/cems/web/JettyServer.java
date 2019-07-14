/*
Freeware License, some rights reserved

Copyright (c) 2019 Iuliana Cosmina

Permission is hereby granted, free of charge, to anyone obtaining a copy 
of this software and associated documentation files (the "Software"), 
to work with the Software within the limits of freeware distribution and fair use. 
This includes the rights to use, copy, and modify the Software for personal use. 
Users are also allowed and encouraged to submit corrections and modifications 
to the Software for the benefit of other users.

It is not allowed to reuse,  modify, or redistribute the Software for 
commercial use in any way, or for a user's educational materials such as books 
or blog articles without prior permission from the copyright holder. 

The above copyright notice and this permission notice need to be included 
in all copies or substantial portions of the software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS OR APRESS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.apress.cems.web;

import com.apress.cems.web.config.WebInitializer;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.AnnotationConfiguration.ClassInheritanceMap;
import org.eclipse.jetty.annotations.ClassInheritanceHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.springframework.web.WebApplicationInitializer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Iuliana Cosmina
 * @since 1.0
 */


class JettyServer {

    private Server server;

    private String name;

    JettyServer(String name) {
        this.name = name;
    }

    void start() throws Exception {
        server = new Server();

        Handler contextHandler = createServletContextHandler(name);
        server.setHandler(contextHandler);

        server.setAttribute("org.mortbay.jetty.Request.maxFormContentSize", 0);
        server.setStopAtShutdown(true);

        ServerConnector connector = new ServerConnector(server);
        connector.setHost("0.0.0.0");
        connector.setPort(8080);
        connector.setIdleTimeout(30000);
        server.setConnectors(new Connector[] { connector });

        // Start the server
        server.start();
        server.join();
    }

    void stop() throws Exception {
        server.stop();
    }

    private ServletContextHandler createServletContextHandler(String name) throws Exception {
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setErrorHandler(null);
        webAppContext.setContextPath("/" + name);
        URI baseUri = getWebRootResourceUri();
        webAppContext.setResourceBase(baseUri.toASCIIString());
        webAppContext.setConfigurations(new Configuration[] {
                new WebXmlConfiguration(),
                new AnnotationConfiguration() {
                    @Override
                    public void preConfigure(WebAppContext context) {
                        ClassInheritanceMap map = createClassMap();
                        context.setAttribute(CLASS_INHERITANCE_MAP, map);
                        _classInheritanceHandler = new ClassInheritanceHandler(map);
                    }
                }
        });

        return webAppContext;
    }

    // finding the webapp directory
    private URI getWebRootResourceUri() throws Exception {
        Path rootPath = FileSystems.getDefault().getPath("");
        Optional<Path>  webAppPathOpt = Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS).filter(t -> {
            File f = t.toFile();
            return f.getName().equalsIgnoreCase("webapp");

        }).findFirst();

        if (webAppPathOpt.isPresent()) {
           return webAppPathOpt.get().toUri();
        }
        throw  new IOException("Could not find 'webapp' directory!");
    }


    private ClassInheritanceMap createClassMap() {
        ClassInheritanceMap classMap = new ClassInheritanceMap();
        Set<String> impl = ConcurrentHashMap.newKeySet();
        impl.add(WebInitializer.class.getName());
        classMap.put(WebApplicationInitializer.class.getName(), impl);
        return classMap;
    }
}
