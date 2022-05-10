from flask import Flask, request, jsonify, render_template, redirect, url_for
# from flask_restx import Resource, Api
from datetime import datetime
import os
import numpy as np
import cv2

app = Flask(__name__)
# api = Api(app)

# 전처리
def pretreatment(file):
    pass

# 예측
def predict():
    pass

@app.route('/')
def hello(): # test
    return render_template("test.html")

@app.route('/predict', methods=['POST'])
def predict():
    save_path = "./data/images/"
    
    # 접근거부로 우선 주석처리
    # if os.listdir(save_path):
    #     os.remove(save_path)

    if os.path.exists(save_path):
        return jsonify(result=0, message='exists')

    files = request.files['img']
    if files:
        file_name = datetime.now().strftime('%Y%H%M%S')
        files.save('{0}{1}.png'.format(save_path, file_name))
        return jsonify(result=1, message='saved')
    else:
        return jsonify(result=0, message='empty file')


if __name__ == "__main__":
    app.run(debug=True) 

# REST API https://problem-solving.tistory.com/9
#  Flask를 사용하여 Python에서 PyTorch를 REST API로 배포 https://tutorials.pytorch.kr/intermediate/flask_rest_api_tutorial.html
#  Flask - 웹서비스 RESTFUL API https://niceman.tistory.com/192
# api 호출: https://toramko.tistory.com/6
# set FLASK_APP=api.py 
# flask run --host=0.0.0.0