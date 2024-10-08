/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core;

import com.google.common.base.Strings;
import com.jamf.regatta.core.encoding.SnappyCodec;
import com.jamf.regatta.core.impl.ClientImpl;
import io.grpc.ClientInterceptor;
import io.grpc.Codec;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SupportedCipherSuiteFilter;
import nl.altindag.ssl.SSLFactory;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ClientBuilder {

    private String target;
    private Duration connectTimeout;
    private NegotiationType negotiationType;
    private List<ClientInterceptor> interceptors;
    private SSLFactory sslFactory;
    private RetryConfig retryConfig = RetryConfig.DEFAULT;


    ClientBuilder() {
    }

    /**
     * configure regatta server endpoint.
     *
     * @param target regatta server target
     * @return this builder to train
     * @throws NullPointerException if target is null or one of endpoint is null
     */
    public ClientBuilder target(String target) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(target), "target can't be null or empty");

        this.target = target;

        return this;
    }

    /**
     * Configure connection timeout.
     *
     * @param connectTimeout Sets the connection timeout.
     * @return this builder
     */
    public ClientBuilder connectTimeout(Duration connectTimeout) {
        if (connectTimeout != null) {
            long millis = connectTimeout.toMillis();
            if ((int) millis != millis) {
                throw new IllegalArgumentException("connectTimeout outside of its bounds, max value: " +
                        Integer.MAX_VALUE);
            }
        }
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Configure negotiationType
     *
     * @param negotiationType negotiationType (TLS / plaintext)
     * @return this builder
     */
    public ClientBuilder negotiationType(NegotiationType negotiationType) {
        this.negotiationType = negotiationType;

        return this;
    }

    /**
     * Override SSLFactory, used in case of NegotiationType.TLS
     *
     * @param sslFactory SSLFactory
     * @return this builder
     */
    public ClientBuilder sslFactory(SSLFactory sslFactory) {
        this.sslFactory = sslFactory;
        return this;
    }

    /**
     * Add an interceptor(s).
     *
     * @param interceptor  an interceptors to add
     * @param interceptors additional interceptors
     * @return this builder
     */
    public ClientBuilder interceptor(ClientInterceptor interceptor, ClientInterceptor... interceptors) {
        if (this.interceptors == null) {
            this.interceptors = new ArrayList<>();
        }

        this.interceptors.add(interceptor);
        this.interceptors.addAll(Arrays.asList(interceptors));

        return this;
    }

    /**
     * The retry configuration. If not set the {@code RetryConfig.DEFAULT} is used.
     *
     * @param retryConfig The retry configuration.
     * @return this builder
     */
    public ClientBuilder retry(RetryConfig retryConfig) {
        Preconditions.checkArgument(retryConfig != null, "retryConfig can't be null");
        Preconditions.checkArgument(retryConfig.unit() != null, "unit can't be null");
        Preconditions.checkArgument(retryConfig.delay() <= 0, "delay must be greater than 0");
        Preconditions.checkArgument(retryConfig.maxDelay() <= 0, "delay must be greater than 0");
        this.retryConfig = retryConfig;
        return this;
    }


    /**
     * build a new Client.
     *
     * @return Client instance.
     */
    public Client build() throws SSLException {
        Preconditions.checkState(target != null, "please configure regatta server endpoints before build.");

        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forTarget(target);

        if (negotiationType != null) {
            channelBuilder.negotiationType(negotiationType);
        }

        if (Objects.equals(negotiationType, NegotiationType.TLS) && sslFactory != null) {
            channelBuilder.sslContext(GrpcSslContexts.configure(toSslContextBuilder(sslFactory)).build());
        }

        if (connectTimeout != null) {
            channelBuilder.withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis());
        }

        if (interceptors != null) {
            channelBuilder.intercept(interceptors);
        }

        var compressorRegistry = CompressorRegistry.newEmptyInstance();
        compressorRegistry.register(Codec.Identity.NONE);
        compressorRegistry.register(SnappyCodec.INSTANCE);
        channelBuilder.compressorRegistry(compressorRegistry);

        var decompressorRegistry = DecompressorRegistry.emptyInstance()
                .with(Codec.Identity.NONE, false)
                .with(SnappyCodec.INSTANCE, true);
        channelBuilder.decompressorRegistry(decompressorRegistry);
        channelBuilder.defaultLoadBalancingPolicy("round_robin");

        return new ClientImpl(channelBuilder.build(), retryConfig);
    }

    private static SslContextBuilder toSslContextBuilder(SSLFactory sslFactory) {
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                .ciphers(sslFactory.getCiphers(), SupportedCipherSuiteFilter.INSTANCE)
                .protocols(sslFactory.getProtocols());
        sslFactory.getKeyManager().ifPresent(sslContextBuilder::keyManager);
        sslFactory.getTrustManager().ifPresent(sslContextBuilder::trustManager);
        return sslContextBuilder;
    }
}
