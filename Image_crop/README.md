# 이미지 CROP 코드 설명

### Library : opencv-python, numpy
### 4가지 단계로 진행
1. 컬러 이미지를 흑백 이미지로 변환
2. 흑백 이미지에 가우시안 블러 적용
3. opencv의 contour 라이브러리 이용해서 contour 후보들 찾음
4. 음료수라고 판단되는 contour 영역만 출력

### 각 단계별 실행 이미지
흑백이미지 대신 원본 이미지로 출력
<img src="/Image_crop/image/gray_cider.PNG"  width=50% height=50%>

