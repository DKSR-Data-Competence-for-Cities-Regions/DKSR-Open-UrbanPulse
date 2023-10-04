package de.urbanpulse.persistence.v3.storage.elasticsearch.clients;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ElasticSearchCustomClientBuilder {

    protected ElasticSearchRestHighLevelClientWrapper client;
    protected String password;
    protected String user;
    protected String httpScheme;
    protected String elasticSearchHost;
    protected boolean verifyHost;
    protected boolean trustAll;
    protected int port;

    public ElasticSearchCustomClientBuilder(String user, String password, String httpScheme,
                                            String elasticSearchHost, int port, boolean trustAll,
                                            boolean verifyHost){
        this.user = user;
        this.password = password;
        this.trustAll = trustAll;
        this.verifyHost = verifyHost;
        this.httpScheme = httpScheme;
        this.elasticSearchHost = elasticSearchHost;
        this.port = port;
    }

    protected RestClientBuilder createRestClient(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
        return RestClient.builder(new HttpHost(elasticSearchHost, port, httpScheme))
                .setHttpClientConfigCallback(callback -> handleHttpClientConfiguration(sslContext, hostnameVerifier));
    }

    protected HttpAsyncClientBuilder handleHttpClientConfiguration(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
        HttpAsyncClientBuilder httpAsyncClientBuilder = HttpAsyncClientBuilder.create();

        if (user != null && password != null) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
            httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        if (httpScheme.equalsIgnoreCase("https")) {
            httpAsyncClientBuilder.setSSLContext(sslContext);
            //if we should skip host verification used in local deployments
            httpAsyncClientBuilder.setSSLHostnameVerifier(hostnameVerifier);
        }

        httpAsyncClientBuilder.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
        return httpAsyncClientBuilder;
    }

    public ElasticSearchRestHighLevelClientWrapper build() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
            //https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/_encrypted_communication.html
            //trust all ssl testing
            final SSLContext sslContext;
            if (trustAll) {
                sslContext = SSLContexts.custom().loadTrustMaterial(new TrustAllStrategy()).build();
            } else {
                //https://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#CustomizingStores
                sslContext = SSLContext.getDefault();
            }

            HostnameVerifier hostnameVerifier;
            if (verifyHost) {
                hostnameVerifier = new DefaultHostnameVerifier();
            } else {
                hostnameVerifier = new NoopHostnameVerifier();
            }

            return new ElasticSearchRestHighLevelClientWrapper(new RestHighLevelClient(createRestClient(sslContext, hostnameVerifier)));
    }
}
