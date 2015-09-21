package com.daedafusion.client.server;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.util.Properties;

/**
 * Created by mphilpot on 6/30/14.
 */
public class BootstrapConfig
{
    private static final Logger log = Logger.getLogger(BootstrapConfig.class);

    private String bootstrapFile;

    public BootstrapConfig()
    {
        bootstrapFile = "classpath:bootstrap.properties";
    }

    public String getBootstrapFile()
    {
        return bootstrapFile;
    }

    /**
     * name.properties => By default assumes to find in classpath
     * classpath:name.properties => Loads from classpath
     * file:///var/lib/name.properties => Loads from filesystem
     *
     * @param bootstrapFile
     */
    public void setBootstrapFile(String bootstrapFile)
    {
        this.bootstrapFile = bootstrapFile;
    }

    public void boot()
    {
        InputStream in = null;

        try
        {

            if (bootstrapFile.startsWith("file://"))
            {
                try
                {
                    in = fromFileSystem(bootstrapFile);
                }
                catch (FileNotFoundException e)
                {
                    log.warn("File Not Found", e);
                }
            }
            else if (bootstrapFile.startsWith("classpath:"))
            {
                in = fromClasspath(bootstrapFile.substring("classpath:".length()));
            }
            else
            {
                in = fromClasspath(bootstrapFile);
            }

            if (in == null)
            {
                log.warn(String.format("Bootstrap properties \"%s\" not found", bootstrapFile));
            }

            Properties properties = new Properties();
            properties.load(in);

            for(String key : properties.stringPropertyNames())
            {
                // If the key is already in System properties, don't override... it probably came in as a -D
                if(System.getProperty(key) == null)
                {
                    System.setProperty(key, properties.getProperty(key));
                }
            }
        }
        catch (IOException e)
        {
            log.warn(String.format("Error loading bootstrap properties from %s", bootstrapFile), e);
        }
        finally
        {
            if(in != null)
            {
                IOUtils.closeQuietly(in);
            }
        }
    }

    public void teardown()
    {
        // Empty
    }

    private InputStream fromClasspath(String fileName)
    {
        return BootstrapConfig.class.getClassLoader().getResourceAsStream(fileName);
    }

    private InputStream fromFileSystem(String path) throws FileNotFoundException
    {
        File f = new File(URI.create(path));
        return new FileInputStream(f);
    }
}
