package org.flutter.plugin.netty;

import android.telecom.ConnectionService;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import javax.xml.transform.Source;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * NettyPlugin
 */
public class NettyPlugin implements FlutterPlugin, MethodCallHandler {
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        final MethodChannel channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "netty");
        channel.setMethodCallHandler(new NettyPlugin());
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "netty");
        channel.setMethodCallHandler(new NettyPlugin());
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("connect")) {
            final String host = call.argument("host");
            final int port = call.argument("port");
            System.out.println("host = " + host + ", port = " + port);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connect(host, port);
                }
            }).start();
            result.success("1");
        } else if (call.method.equals("send")) {
            String msg = call.argument("msg");
            System.out.println("msg = " + msg);
            send(msg);
        } else if (call.method.equals("close")) {
            close();
            result.success("1");
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    }

    private ChannelFuture mChannelFuture;

    // 连接
    private void connect(final String host, final int port) {
        System.out.println("---------------- connect ----------------");
        // 初始化线程组
        NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class).group(nioEventLoopGroup);
        // 非阻塞
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        // 长连接
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        // 超时时间
        // bootstrap.option(ChannelOption.SO_TIMEOUT, 10000);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                pipeline.addLast("decode", new StringDecoder());
                pipeline.addLast("encode", new StringEncoder());
                pipeline.addLast("handler", new ChannelHandle());
            }
        });
        // 建立连接
        try {
            ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port)).sync();
            this.mChannelFuture = future;
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("---------------- 客户端连接资源释放 ----------------");
    }

    // 发送
    private void send(String msg) {
        System.out.println("---------------- send ----------------");
        ChannelFuture future = this.mChannelFuture;
        if (future == null) {
            return;
        }
        Channel channel = future.channel();
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(msg + "\n");
        }
    }

    // 关闭
    private void close() {
        System.out.println("---------------- close ----------------");
        ChannelFuture future = this.mChannelFuture;
        if (future == null) {
            return;
        }
        Channel channel = future.channel();
        if (channel != null) {
            channel.close();
        }
    }
}
