# 이미지 CROP 코드 설명

### Library : opencv-python, numpy
### 4가지 단계로 진행
- 컬러 이미지를 흑백 이미지로 변환
<img src="/Image_crop/image/gray_cider.PNG"  width=25% height=25%>

- 흑백 이미지에 가우시안 블러 적용
<img src="/Image_crop/image/gaussian_cider.PNG"  width=25% height=25%>

- opencv의 contour 라이브러리 이용해서 contour 후보들 찾음
<img src="/Image_crop/image/contour_cider.PNG"  width=25% height=25%>

- 음료수라고 판단되는 contour 영역만 출력
<img src="/Image_crop/image/result_cider.PNG"  width=25% height=25%>
