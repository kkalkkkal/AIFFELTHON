from flask import Flask, request, jsonify, render_template, redirect, url_for
from datetime import datetime
import os

app = Flask(__name__)

@app.route('/')
def hello(): # test
    return render_template("test.html")

@app.route('/predict', methods=['POST'])
def predict():
    path = "./data/images/"

    # 접근거부로 우선 주석처리
    # if os.listdir(path):
    #     os.remove(path)

    files = request.files['img']
    if files:
        file_name = datetime.now().strftime('%Y%H%M%S')
        files.save('{0}{1}.png'.format(path, file_name))
        return jsonify(result=1, message='saved')
    else:
        return jsonify(result=0, message='empty file')

    # return redirect(url_for('hello'))
    # return jsonify(result=result, message=message)

if __name__ == "__main__":
    app.run(debug=True) 

# REST API https://problem-solving.tistory.com/9
#  Flask를 사용하여 Python에서 PyTorch를 REST API로 배포 https://tutorials.pytorch.kr/intermediate/flask_rest_api_tutorial.html
#  Flask - 웹서비스 RESTFUL API https://niceman.tistory.com/192
# api 호출: https://toramko.tistory.com/6
# set FLASK_APP=api.py 
# flask run --host=0.0.0.0