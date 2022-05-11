from fileinput import filename
from flask import Flask, request, jsonify, render_template
import os
import numpy as np
from collections import OrderedDict
from datetime import datetime
from werkzeug.utils import secure_filename
from easyocr import Reader
# from PIL import Image

import torch
import torch.backends.cudnn as cudnn
import torch.utils.data
import torch.nn.functional as F

app = Flask(__name__)

device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

def get_files(path):
    file_list = []

    files = [f for f in os.listdir(path) if not f.startswith('.')]  # skip hidden file
    files.sort()
    abspath = os.path.abspath(path)
    for file in files:
        file_path = os.path.join(abspath, file)
        file_list.append(file_path)

    return file_list, len(file_list)

@app.route('/')
def get():
    return render_template("test.html")

@app.route('/image', methods=['POST'])
def image(): 
    save_path = "./workspace/demo_images/"
    model_path = "./workspace/user_network_dir"

    # 접근거부로 우선 주석처리
    # if os.listdir(save_path):
    #     os.remove(save_path)

    date = request.form['date']
    category = request.form['category']
    files = request.files.getlist("images[]")
    for f in files:
        file_name = datetime.now().strftime('%Y%H%M%S')+".jpeg"
        f.save(save_path+secure_filename(file_name))
    
    reader = Reader(['ko'], gpu=True,
                    model_storage_directory=model_path,
                    user_network_directory=model_path,
                    recog_network='custom')

    # files, count = get_files(save_path)
    return jsonify("ss")


if __name__ == "__main__":
    app.run(debug=True) 

# set FLASK_APP=api.py 
# flask run --host=0.0.0.0