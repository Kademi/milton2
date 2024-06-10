package io.milton.mail.receive;

/**
 *
 * @author brad
 */
public interface SmtpServer {

    void start();

    void stop();

    int getSmtpPort();
    
    void setHostname(String hostname);
    
    void setEnableProxyProtocolV2(Boolean proxyProtocolV2Enabled);
}
