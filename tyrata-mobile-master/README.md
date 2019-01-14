# Tyrata Android App
## Installation
1. Clone or download the project
2. Open the project with Android Studio
3. Build the project
4. Run on Emulator or physical Android device

## Specs
The app targets (Android 4.2 JellyBean API 17 and later),
which is 96% of Android devices on market.

## Activities
#### 1. User activity (registration and log-in)
    For new user, you should register with your user information and then login.
#### 2. Main activity (main view navigation)
    In the main activity page, you can see:
* account information
* report accident
* add cehicle
* get bluetooth data
* vehicle list
* Sign Out
    
#### 3. Calibration activity (vehicle and tire)
##### * add cehicle
    Click the "add cehicle" button in the main page, you can add six types of vehicles: four-wheel, six-wheel, eight-wheel, ten-wheel, forteen-wheeland eighteen-wheel. 
    After you add a new vehicle, you'll be directed to main page and then click on the added vehicle in the list, you are able to calibrate/edit the 
    vehicle information, delete the vehicle or add tires to this vehicle.
##### * add tire
    Click on the tire you want to calibrate in the list of respective vehicle, you're able to calibrate/edit the tire information or delete this tire.

#### 4. Storage activity (local store and load)
    Everytime you add new vehicle/tire, the data will be stored in the local database as well as sent through HTTP to the server. 
    Once you get to the main page, the app will automatically sync data with the server

#### 5. Bluetooth activity (discover, connect, and transmit/receive)
##### * Discover and connect
    Click on "get bluetooth data"
    You can see the paired device in the first part and the discovered devices in the second part, which you can connect the devices in the second part.
    Here you will get the snapshot sent from the simulator. The app will process the raw data and save them in local database and send them to the server.

#### 6. GPS activity (get device GPS)
    The GPS location will be saved along with the snapshot.

#### 7. HTTP activity (send/receive data to/from the database)
    The local database syncs with the server through HTTP.

## Communications
* The app communicates with the simulator through bluetooth
* The app communicates with the database through HTTP
* The message uses XML format, details for the message format can be found:
*     Bluetooth: https://docs.google.com/document/d/19h3uDUiwvbqEmfie1sN9KuPJ4GxLfGlR5GvPxORzuBs/edit?usp=sharing
*     HTTP: https://docs.google.com/document/d/1m62Nkwoneb2JllnBDGVYYLbOiGOqPR6h3vGn0bQhrZc/edit?usp=sharing