import cv2
import numpy as np

# contour 영역의 최소 크기
MIN_AREA = 1000

# 테스트 원하는 이미지 경로를 입력해주면 됩니다.
img = cv2.imread("C:/Users/mtr09/Desktop/test/test/cider1.jpeg")
# 원본 이미지가 크기 때문에 이미지 축소
small_img = cv2.resize(img, dsize=(0, 0), fx=0.2, fy=0.2, interpolation=cv2.INTER_LINEAR)
height, width, channel = small_img.shape
img2 = small_img.copy()
crop_img = img2.copy()

# 컬러 이미지를 흑백 이미지로
img_gray = cv2.cvtColor(img2, cv2.COLOR_BGR2GRAY)
# 가우시안 필터 적용한 이미지
gaussian_img = cv2.GaussianBlur(img_gray, ksize=(5, 5), sigmaX=0)

# 원본 이미지 출력
cv2.imshow('original image', small_img)
cv2.waitKey(0)
cv2.destroyAllWindows()

# 이미지의 노이즈를 제거하기 위해서 가우시안 필터 적용
gaussian_img_thresh = cv2.adaptiveThreshold(
    gaussian_img,
    maxValue=255.0,
    adaptiveMethod=cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
    thresholdType=cv2.THRESH_BINARY_INV,
    blockSize=19,
    C=9
)

# 가우시안 필터 적용한 이미지 출력
cv2.imshow('gaussian img', gaussian_img_thresh)
cv2.waitKey(0)
cv2.destroyAllWindows()

# contours 적용(contour 란 동일한 색 또는 동일한 픽셀값을 가지고 있는 영역의 경계선 정보)
contours, hierarchy = cv2.findContours(gaussian_img_thresh, mode=cv2.RETR_LIST, method=cv2.CHAIN_APPROX_SIMPLE)

temp_result = np.zeros((height, width, channel), dtype=np.uint8)

contours_dict = []

# contour 는 정보를 정제하는 과정 x, y, w, h 의 좌표로 구성돼있음.
for contour in contours:
    x, y, w, h = cv2.boundingRect(contour)
    cv2.rectangle(temp_result, pt1=(x,y), pt2=(x+w, y+h), color=(255, 255, 255), thickness=2)

    contours_dict.append({
        'contour' : contour,
        'x' : x,
        'y' : y,
        'w' : w,
        'h' : h,
        'cx' : x + (w / 2),
        'cy' : y + (h / 2)
    })

# contour 들을 추출한 이미지 출력
cv2.imshow('Contours', temp_result)
cv2.waitKey(0)
cv2.destroyAllWindows()

possible_contours = []


cnt = 0
# 최대값 정보 담기 위한 변수들
# 최대 영역의 크기, x좌표, y좌표, 너비, 높이
max_area, max_x, max_y, max_w, max_h = 0, 0, 0, 0, 0

for d in contours_dict:
    # contour 영역의 넓이
    area = d['w'] * d['h']
    # contour 영역 너비와 높이의 비율(너비 / 높이)
    ratio = d['w'] / d['h']

    # 최소 영역 크기보다 큰 contour 만 비교대상임
    if area > MIN_AREA:
        # 가장 큰 영역 정보 저장
        if area > max_area:
            max_area = area
            max_x = d['x']
            max_y = d['y']
            max_w = d['w']
            max_h = d['h']

        print("width", d['w'], "height", d['h'])
        d['idx'] = cnt
        cnt += 1
        possible_contours.append(d)

# 최대 영역의 contour 정보 출력
print("max x", max_x, "max y", max_y, "max_w", max_w, "max_h", max_h)

temp_result = np.zeros((height, width, channel), dtype=np.uint8)

# contour 영역중 가장 큰 영역만 나오게 하기위해
# 해당 픽셀 영역의 픽셀 정보만 temp_result에 저장 후 출력
for x in range(width):
    for y in range(height):
        if x >= max_x and x <= max_x + max_w and y >= max_y and y <= max_y + max_h:
            temp_result[y][x] = crop_img[y][x]

# 음료수가 있을 것이라고 예상되는 영역의 정보가 temp_result 에 있음.
# temp_result 는 (height, width, channel)로 구성

# 음료수가 있을 것이라고 예상되는 영역을 추출한 이미지 출력
cv2.imshow('Contours', temp_result)
cv2.waitKey(0)
cv2.destroyAllWindows()
