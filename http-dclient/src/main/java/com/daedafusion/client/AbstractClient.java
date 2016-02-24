package com.daedafusion.client;

import com.daedafusion.configuration.Configuration;
import com.daedafusion.crypto.Crypto;
import com.daedafusion.crypto.CryptoFactory;
import com.daedafusion.discovery.Discovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.security.KeyStore;
import java.util.Map;

/**
 * Created by mphilpot on 8/13/14.
 */
public abstract class AbstractClient implements Closeable
{
    private static final Logger log = Logger.getLogger(AbstractClient.class);

    protected static final String ACCEPT = "accept";
    protected static final String CONTENT = "content-type";
    protected static final String AUTH = "authorization";

    protected static final String TEXT_XML = "text/xml";
    protected static final String TEXT_PLAIN = "text/plain";
    protected static final String APPLICATION_JSON = "application/json";

    protected final String serviceName;
    protected URI baseUrl;
    protected CloseableHttpClient client;

    private String authToken;

    protected AbstractClient(String serviceName)
    {
        this(serviceName, null, null);
    }

    protected AbstractClient(String serviceName, String url)
    {
        this(serviceName, url, null);
    }

    protected AbstractClient(String serviceName, String url, CloseableHttpClient client)
    {
        this.serviceName = serviceName;

        if(url == null && Configuration.getInstance().getBoolean("useDiscovery", true))
        {
            try
            {
                ServiceInstance instance = Discovery.getInstance().getInstance(serviceName);
                baseUrl = URI.create(instance.buildUriSpec());
            }
            catch (Exception e)
            {
                log.error("", e);
                throw new RuntimeException(e);
            }
        }
        else if(url == null)
        {
            baseUrl = URI.create(Configuration.getInstance().getString(String.format("%sUrl", serviceName), null));
        }
        else
        {
            this.baseUrl = URI.create(url);
        }

        if(client == null)
        {
            if(baseUrl.toString().startsWith("https"))
            {
                SSLConnectionSocketFactory sslFactory = null;
                SSLContext sslContext = null;
                try
                {
                    KeyStore keyStore = CryptoFactory.getInstance().getKeyStore();

                    sslContext = SSLContexts.custom()
                            .loadKeyMaterial(keyStore, Crypto.getProperty(Crypto.KEYSTORE_PASSWORD).toCharArray(), new PrivateKeyStrategy()
                            {
                                @Override
                                public String chooseAlias(Map<String, PrivateKeyDetails> aliases, Socket socket)
                                {
                                    return Crypto.getProperty(Crypto.SERVICE_CERT_ALIAS);
                                }
                            })
                            .useTLS()
                            .loadTrustMaterial(keyStore)
                            .build();

                    sslFactory = new SSLConnectionSocketFactory(
                            sslContext,
                            SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER
                    );
                }
                catch (Exception e)
                {
                    log.error("", e);
                }

                this.client = HttpClients.custom()
                        .setSSLSocketFactory(sslFactory)
                        .useSystemProperties()
                        .build();
            }
            else
            {
                this.client = HttpClients.createSystem();
            }
        }
        else
        {
            this.client = client;
        }
    }

    public String getAuthToken()
    {
        return authToken;
    }

    public void setAuthToken(String authToken)
    {
        this.authToken = authToken;
    }

    @Override
    public void close() throws IOException
    {
        if(client != null)
        {
            client.close();
        }
    }
}
