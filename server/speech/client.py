# Copyright (c) 2021, NVIDIA CORPORATION.  All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import argparse
import tritonclient.grpc as grpcclient
from speech_client import *

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-v',
                        '--verbose',
                        action="store_true",
                        required=False,
                        default=False,
                        help='Enable verbose output')
    parser.add_argument('-u',
                        '--url',
                        type=str,
                        required=False,
                        default='localhost:8001',
                        help='Inference server URL. Default is '
                             'localhost:8001.')
    FLAGS = parser.parse_args()

    # load data
    audio_file = 'output.wav'
    speech_client_cls = OfflineSpeechClient
    model_name = 'attention_rescoring'

    # tasks
    predictions = []
    tasks = (0, [audio_file])

    # 开始进行推理
    with grpcclient.InferenceServerClient(url=FLAGS.url,
                                          verbose=FLAGS.verbose) as triton_client:
        protocol_client = grpcclient
        speech_client = speech_client_cls(triton_client, model_name,
                                          protocol_client, FLAGS)
        idx, audio_files = tasks

        for li in audio_files:
            result = speech_client.recognize(li, idx)
            predictions += result

    print("识别结果为：", predictions[0])

