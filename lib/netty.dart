import 'dart:async';

import 'package:flutter/services.dart';

class Netty {
  static const MethodChannel _channel = const MethodChannel('netty');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  // 连接
  static Future<String> connect(String host, int port) async {
    return await _channel.invokeMethod('connect', {'host': host, 'port': port});
  }

  // 发送数据
  static Future<String> send(String msg) async {
    return await _channel.invokeMethod('send', {'msg': msg});
  }

  // 关闭
  static Future<String> close() async {
    return await _channel.invokeMethod('close');
  }
}
