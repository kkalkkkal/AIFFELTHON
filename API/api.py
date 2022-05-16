from flask import Flask, request, jsonify, render_template
import os
import json
from collections import OrderedDict
from datetime import datetime
from easyocr.easyocr import *

app = Flask(__name__)

# GPU 설정
os.environ['CUDA_VISIBLE_DEVICES'] = '0,1'

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
def post(): 

    save_path = "./workspace/demo_images/"
    model_path = "./workspace/user_network_dir"

    # 접근거부로 우선 주석처리
    # if os.listdir(save_path):
    #     os.remove(save_path)

    # save image
    date = request.form['date']
    category = request.form['category']
    files = request.files.getlist("images[]")
    for f in files:
        file_name = date+"_"+category+"_"+f.filename.split('.')[0]+".jpeg"
        f.save(save_path+file_name)

    # model
    reader = Reader(['en'], gpu=True,
                    model_storage_directory=model_path,
                    user_network_directory=model_path,
                    recog_network='custom')

    # json
    json_data = OrderedDict()
    json_list = []
    
    files, count = get_files(save_path)
    
    for idx, file in enumerate(files):
        filename = os.path.basename(file)
        result = reader.readtext(file)

        # ./easyocr/utils.py 733 lines
        # result[0]: bbox
        # result[1]: string
        # result[2]: confidence
        for (bbox, string, confidence) in result:
            print("filename: '%s', confidence: %.4f, string: '%s'" % (filename, confidence, string))
            #json
            json_data["img_name"] = str(filename)
            json_data["time"] = str(datetime.now())
            json_data["confience"] = str(confidence)
            json_data["string"] = str(string)
            json_data["category"] = str(filename.split('_')[1])
            
            json_list.append(json.dumps(json_data, ensure_ascii=False, indent="\t"))

    return jsonify(json_list)



if __name__ == "__main__":
    app.run(debug=True) 
    