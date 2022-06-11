# AIFFELTHON
아이펠 양재 2기 해커톤, 주제-유통기한 리더기(OCR) 


# 개요
저시력자들의 식중독 문제를 예방하기 위해 유통기한을 손쉽게 확인할 수 있는 어플 개발

<br>

# 프로젝트 소개
<img width="80%" src="https://user-images.githubusercontent.com/63850490/172521378-ea3e1dbf-38e9-4826-9e74-fec97ba9ba43.png"/>

# 프로젝트 동기 
<img width="80%" src="https://user-images.githubusercontent.com/63850490/172521396-d93f2c8c-98ba-41c2-840a-bfbc66f87e50.png"/>

유튜브 알고리즘에 의해 시각장애인 유튜버 <원샷한솔>님의 한 콘텐츠를 보게 되었다. <br>
‘시각장애인들은 어떻게 쇼핑을 할까?’라는 것이 바로 그 콘텐츠의 주제였는데, 그 영상을 통해서 저시력자의 고충을 알게 되었다.

<br>

이와 관련해 한국시각장애인연합회 한국웹접근성평가센터에서의 면담을 통해 <br>
시각 장애인들이 물건을 사는 데에도 불편함이 있지만 그 물건들을 집에 와서 각각이 어떤 물건인지 <br>
그리고 유통기한이 언제까지인지 인지하는데 불편함이 있다는 것을 알게 되었다.

<br><br>
저시력자 분들이 유통기한을 보지 못해 식중독에 걸리는 사례가 잦아 이 문제를 해결하기 위해 해당 프로젝트를 기획하였다. 

<br><br>


# 프로젝트 주요 기능
<img width="80%" src="https://user-images.githubusercontent.com/63850490/172521395-4fe971b7-8aa6-4298-9b94-839452de8704.png"/>

<br><

# 사용 시나리오
<img width="80%" src="https://user-images.githubusercontent.com/63850490/172521390-b255bab7-277f-4346-8128-7fdbfc7952db.png"/>

1. 냉장고에서 손을 더듬어 우유곽을 집는다
2. 우유곽의 상단을 촉각으로 찾아낸다
3. (Talkback 이나 빅스비 음성 보조 AI를 통해) 앱을 켜고 카메라를 갖다 댄다
4. 카메라를 통해 이미지 정보를 받은 앱이 알아서 우유곽을 인식하고, 유통기한 영역을 찾아 사용자에게 유통기한 정보를 음성으로 제공한다


<br><br>

# 개발 세부사항 

<br> <br>

## 시스템 설계도
<img width="80%" src="https://user-images.githubusercontent.com/63850490/172522531-377fd44b-3a18-46f5-ac70-429ecff921a1.png"/>

- GCP Server => Flask (HTTP 기반 RESTful API )
- Machine Learning(ML) : Object Detection(OD) 
  1. Roboflow : bounding box(경계 박스)를 설정하고 데이터셋을 만드는 무료 도구이다. 학습용 데이터를 roboflow에 적재하고 경계 박스를 설정하여 데이터셋을 추출할 수 있다. 
  2. YOLOv4 darknet : Object Detection을 위한 YOLO 알고리즘. YOLOv4 darknet 오픈소스를 활용하여 학습을 진행한다. <br> 그 데이터를 바탕으로 만들어낸 모델을 경량화 시켜 Tensorflow Lite의 객체 탐지 알고리즘에 적용한다.

- EasyOCR : 무료 OCR 오픈소스. 따로 학습하여 개량한 EasyOCR로 탐지된 각 텍스트 영역별 이미지에서 텍스트 데이터를 추출한다

- Google TTS : EasyOCR로 추출한 텍스트 정보를 전달하여 사용자에게 음성정보를 제공한다. 

- Google ASR : 이와 별도로 제스처와 ASR(Auto Speak Response)기능을 통해 음성으로도 어플 조작을 가능하게 한다.



<br><br>

# 개선 사항 

## 유통기한 탐지 모델링
<img width="80%" src="https://user-images.githubusercontent.com/63850490/172521388-fd6f50c3-fd96-4490-87dc-82d98d1b87a0.png"/>

기존 EfficientDet으로 만든 모델보다 YOLOv4의 darknet이 더 실시간성의 성능면에서 적합함.
<br>
Darknet으로 학습해서 얻어낸 가중치 weight을 tensorflow lite에서 사용될 수 있도록 tflite로 전환함.

- mAP : 27.7% -> mAP : 62.6%, 총  34.9% 개선


## OCR 개선
[기존 OCR 모델을 개선하려고 했으나 초기 모델보다 서비스할 수준의 퍼포먼스를 확보하지 못함. 
](https://www.notion.so/modulabs/OCR-3b7f79c4984f48028a01ff5abe664405)

그래서 초기 모델을 그대로 사용.

<br><br><br>


## 참고자료 
### OCR 학습 <br>
https://citrine-cashew-534.notion.site/OCR-35792a9663de4b5eab99bee838946bde

<br>

### 데이터 셋

우유곽 데이터셋 : 직접 수집함

한글 데이터 AI hub : [https://aihub.or.kr/aidata/133](https://aihub.or.kr/aidata/133)

텍스트 생성기 : [https://textrecognitiondatagenerator.readthedocs.io/en/latest/index.html](https://textrecognitiondatagenerator.readthedocs.io/en/latest/index.html)

형상 변환기 : [https://github.com/DaveLogs/TRDG2DTRB](https://github.com/DaveLogs/TRDG2DTRB)

<br>

### 프로토타입 동작 

https://www.youtube.com/embed/mOdvTOchP9I
