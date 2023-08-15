const http = require("http")
const path = require("path");
const WebSocket = require("websocket").server;
const wav = require("wav");
const fs = require("fs");
const messages = require("./message.js");

// 假设每个音频文件的采样率、通道数和样本宽度都相同
const SAMPLE_RATE = 16000;
const CHANNELS = 1;
const SAMPLE_WIDTH = 2; // 16 bits per sample

let outputStream = null;
let data; // 语音转文字结果
let statusMessageInterval = null;

// 创建一个 HTTP 请求对象，检测是否启动 Triton
const options = {
    host: 'localhost',
    port: 8000,
    path: '/v2/health/ready',
    method: 'GET',
    headers: {
        'User-Agent': 'curl/7.68.0',
        'Accept': '*/*'
    }
};

function startServer() {
    const server = require("http").createServer((request, response) => {
        response.writeHead(404);
        response.end();
    });

    const { spawn } = require('child_process')

    server.listen(8080, () => {
        console.log("Server is listening on port 8080");
    });

    const wsServer = new WebSocket({
        httpServer: server,
        autoAcceptConnections: true,
    });

    wsServer.on("connect", (connection) => {
        console.log("WebSocket connection accepted");
        connection.sendUTF(JSON.stringify(messages.statusMessage1))     // 发送服务器连接成功的消息给客户端

        outputStream = new wav.FileWriter(path.join(__dirname, "output.wav"), {
            channels: CHANNELS,
            sampleRate: SAMPLE_RATE,
            bitDepth: SAMPLE_WIDTH * 8,
        });

        // 检测是否开启 Triton 服务器
        const req = http.request(options, res => {
            connection.on("message", (message) => {
                if (message.type === "binary") {
                    outputStream.write(message.binaryData);
                }
                // 录音结束
                else {
                    // 检测 NX 是否开启 Triton
                    connection.sendUTF(JSON.stringify(messages.statusMessage3))     // 发送接收成功的消息给客户端
                    // 接收客户端发送的字符串消息
                    let str = JSON.stringify(message);
                    str = JSON.parse(str).utf8Data;
                    console.log(`Received message from client: ${str}`);
                    // 运行 Python 子进程，音频程序进行推理
                    const pythonProcess = spawn('python', ['speech/client.py'])
                    pythonProcess.stdout.on('data', function(res){
                        data = res.toString();
                        console.log(data)
                        messages.resultMessage.data = data
                        connection.sendUTF(JSON.stringify(messages.resultMessage))  // 发送识别结果给客户端
                    })
                    pythonProcess.on('close', () => {
                        // 将推理结果写入到文件
                        fs.writeFile('result.txt', data, (err) => {
                            if (err) throw err;
                            console.log("The file has been saved!")
                        });
                        connection.sendUTF(JSON.stringify(messages.stopMessage))     // 发送关闭连接的消息给客户端
                    })
                }
            });
        });

        req.on('error', error => {
            console.error(error);
            console.log("Triton 服务器未启动")
            connection.sendUTF(JSON.stringify(messages.statusMessage4));
        })

        req.end();

        connection.on("close", () => {
            console.log("WebSocket connection closed.");
        });
    });
}


startServer();
