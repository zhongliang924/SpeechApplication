// 定义消息模块

const messages = {
    resultMessage: {
        type: 'result',
        data: [],
    },
    statusMessage1: {
        type: 'status',
        status: '服务器连接成功',
    },
    statusMessage2: {
        type: 'status',
        status: '服务器正在接收录音',
    },
    statusMessage3: {
        type: 'status',
        status: '接收录音完成，等待接收语音转文字结果',
    },
    statusMessage4: {
        type: 'status',
        status: 'Triton 服务器未启动',
    },
    stopMessage: {
        type: 'stop',
        message: '关闭连接'
    }
}

module.exports = messages;