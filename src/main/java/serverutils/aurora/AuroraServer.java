package serverutils.aurora;

import java.io.PrintWriter;
import java.io.StringWriter;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import serverutils.aurora.page.HomePage;
import serverutils.aurora.page.WebPage;
import serverutils.aurora.page.WebPageNotFound;
import serverutils.aurora.page.WebPageUnauthorized;

public class AuroraServer {

    private final MinecraftServer server;
    private final int port;
    private ChannelFuture channel;
    private final EventLoopGroup masterGroup;
    private final EventLoopGroup slaveGroup;

    private byte[] iconBytes = null;

    public AuroraServer(MinecraftServer s, int p) {
        server = s;
        port = p;
        masterGroup = new NioEventLoopGroup();
        slaveGroup = new NioEventLoopGroup();
    }

    public MinecraftServer getServer() {
        return server;
    }

    void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(masterGroup, slaveGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

                @Override
                public void initChannel(final SocketChannel ch) {
                    ch.pipeline().addLast("codec", new HttpServerCodec());
                    ch.pipeline().addLast("aggregator", new HttpObjectAggregator(512 * 1024));
                    ch.pipeline().addLast("request", new ChannelInboundHandlerAdapter() {

                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            if (msg instanceof FullHttpRequest) {
                                handleRequest(ch, ctx, (FullHttpRequest) msg);
                            } else {
                                super.channelRead(ctx, msg);
                            }
                        }

                        @Override
                        public void channelReadComplete(ChannelHandlerContext ctx) {
                            ctx.flush();
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                            ctx.writeAndFlush(
                                    new DefaultFullHttpResponse(
                                            HttpVersion.HTTP_1_1,
                                            HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                            Unpooled.copiedBuffer(cause.getMessage().getBytes())));
                        }
                    });
                }
            });

            bootstrap.option(ChannelOption.SO_BACKLOG, 128);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            channel = bootstrap.bind(port).sync();
        } catch (InterruptedException ignored) {}
    }

    void shutdown() {
        slaveGroup.shutdownGracefully();
        masterGroup.shutdownGracefully();

        try {
            channel.channel().closeFuture().sync();
        } catch (InterruptedException ignored) {}
    }

    private void handleRequest(SocketChannel channel, ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.getUri();
        WebPage page;

        while (uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        while (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }

        if (uri.isEmpty()) {
            page = new HomePage(this);
        } else {
            try {
                AuroraPageEvent event = new AuroraPageEvent(this, request, uri);
                MinecraftForge.EVENT_BUS.post(event);
                page = event.getPage();

                if (page != null && page.getPageType() != PageType.ENABLED
                        && !System.getProperty("AuroraIgnoreAuth", "0").equals("1")) {
                    page = new WebPageUnauthorized(event.getUri());
                }

                if (page == null) {
                    page = new WebPageNotFound(event.getUri());
                }
            } catch (Exception ex) {
                page = new WebPageNotFound("errored");
                ex.printStackTrace();
            }
        }

        String content;
        String contentType;

        try {
            content = page.getContent();
            contentType = page.getContentType();
        } catch (Exception ex) {
            ex.printStackTrace();
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            printWriter.println("Error!");
            printWriter.println();
            ex.printStackTrace(printWriter);
            content = writer.toString();
            contentType = "text/plain";
        }

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                page.getStatus(),
                Unpooled.copiedBuffer(content.getBytes()));

        if (HttpHeaders.isKeepAlive(request)) {
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, content.length());
        response.headers().set(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        ctx.writeAndFlush(response);
    }

    public boolean allow(String uri) {
        return true;
    }
}
