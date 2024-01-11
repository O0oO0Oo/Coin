# Coin
비트코인 프로젝트 리팩토링

[자세한 내용 노션 링크](https://sungwon9.notion.site/Coin-8724855a1d8a45a38b4d693734be0144?pvs=4)

## 아키텍처
초기 구성은 다음과 같이 계획하였지만, 테스트 후에 변경할 예정입니다.

<img src="https://github.com/O0oO0Oo/Coin/assets/110446760/659c74bc-0e7e-4e40-8226-2c54e71c30ca" width="700">

### 중요 요구사항
- (Price Module) 최소 1초에 4번 모든 코인의 가격을 받아와야 한다.
- (Trade Module) 최소 1초에 2번 모든 종류의 코인 가격에대해 매치되는 주문을 검색하고 처리해야한다.

## 모듈의 동작 과정

### Price Module
가격 정보를 받아오는 모듈입니다.

```@Scheduled```, ```@Async``` 를 사용하여 요구사항에 맞게 구현하였습니다.

<img src="https://github.com/O0oO0Oo/Coin/assets/110446760/89d51550-b664-402f-98e6-8187369c0463" width="550">

### Trade Module
Price 모듈로부터 받아온 가격정보와 매치되는 주문들을 검색하여 거래하는 모듈입니다.

```이벤트루프``` 구조를 모방하여 만들었으며, Redis 에서 ```비동기```로 데이터를 받아와 성공/실패에 따라 이벤트를 발행하고 큐에 저장하게 됩니다.

<img src="https://github.com/O0oO0Oo/Coin/assets/110446760/d4fb48fe-8adb-4c20-975a-71990ef645f0" width="900">

### User Module
Price, Trade 모듈을 통해 주문등록, 아래와 같이 처리된 주문의 저장 그리고 전반적인 유저의 요청들을 처리합니다.

<img src="https://github.com/O0oO0Oo/Coin/assets/110446760/9f3f6b12-6aa5-4867-aa2b-3480bbdd8ef0" width="550">

### Issue
1. 처음 계획했던 루프 구조 문제점 문제해결 과정 : [Issue-26](https://github.com/O0oO0Oo/Coin/issues/26)
   
### ERD
- crypto : 코인 종류
- wallet : 유저 보유 코인지갑
- sell/buy_order : 유저의 판매/구매
- user : 사용자
<img src="https://github.com/O0oO0Oo/Coin/assets/110446760/74aa375c-abba-4342-adcc-e407e391e169" width="550">
