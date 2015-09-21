package com.daedafusion.client.server;

import com.daedafusion.configuration.Configuration;
import com.daedafusion.crypto.Crypto;
import com.daedafusion.crypto.CryptoFactory;
import com.daedafusion.discovery.DiscoveryRegistrationListener;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.servlet.ServletContainer;

import java.security.KeyStore;

/**
 * Created by mphilpot on 8/14/14.
 */
public class PingServer implements Runnable
{
    private static final Logger log = Logger.getLogger(PingServer.class);

    private boolean useSsl = false;

    public PingServer()
    {
        new BootstrapConfig().boot();
    }

    public boolean isUseSsl()
    {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl)
    {
        this.useSsl = useSsl;
    }

    @Override
    public void run()
    {
        Server server = new Server();
        server.setStopAtShutdown(true);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        DiscoveryRegistrationListener drl = new DiscoveryRegistrationListener(
                Configuration.getInstance().getString("serviceName"),
                "localhost",
                8443,
                true
        );

        context.addEventListener(drl);

        server.setHandler(context);

        ServletHolder h = new ServletHolder(new ServletContainer());
        h.setInitParameter("javax.ws.rs.Application", Application.class.getName());

        context.addServlet(h, "/*");

        if(useSsl)
        {
            HttpConfiguration https_config = new HttpConfiguration();
            https_config.setSecureScheme("https");
            https_config.setSecurePort(8443);
            https_config.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setNeedClientAuth(true);

            try
            {
                KeyStore keyStore = CryptoFactory.getInstance().getKeyStore();

                sslContextFactory.setTrustStore(keyStore);
                sslContextFactory.setTrustStorePassword(Crypto.getProperty(Crypto.KEYSTORE_PASSWORD));

                sslContextFactory.setKeyStore(keyStore);
                sslContextFactory.setKeyStorePassword(Crypto.getProperty(Crypto.KEYSTORE_PASSWORD));
                sslContextFactory.setKeyManagerPassword(Crypto.getProperty(Crypto.PROTECTION_PASSWORD));
            }
            catch (Exception e)
            {
                log.error("", e);
            }

            ServerConnector connector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https_config));
            connector.setPort(8443);
            server.addConnector(connector);
        }
        else
        {
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(8080);
            server.addConnector(connector);
        }

        try
        {
            server.start();
            server.join();
        }
        catch(Throwable t)
        {
            log.info("Shutting Down", t);
            try
            {
                server.stop();
            }
            catch (Exception e)
            {
                log.error("", e);
            }
        }
    }
}
