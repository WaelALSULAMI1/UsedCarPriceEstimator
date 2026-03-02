# UsedCarPriceEstimator
Java project integrating JDBC, Multithreading, Socket Programming and GUI for used car price estimation in SAR.
# 🚗 Used Car Price Estimator (SAR)

A Java-based desktop application that estimates used car prices in Saudi Riyals (SAR).

## 📌 Features

- 💰 Realistic pricing model
  - 5% yearly depreciation (compound)
  - Mileage impact (1% per 10,000 km)
  - Minimum price cap at 20% of base value
- 🌍 Case-insensitive car search (Toyota, toyota, TOYOTA all work)
- 🗄 SQLite database integration
- 🌐 Market adjustment factor via local networking server
- 🧵 Multi-threaded price calculation
- 📝 Estimation logging system

## 🛠 Technologies Used

- Java (JDK 21)
- Swing (GUI)
- SQLite (JDBC)
- Multi-threading (ExecutorService)
- Socket Networking

## ▶ How to Run

1. Compile all `.java` files
2. Ensure `sqlite-jdbc` is available
3. Run:

## 📷 Example

Enter:
- Make: Toyota
- Model: Camry
- Year: 2020
- Mileage: 180000

The system calculates:
- Age depreciation
- Mileage depreciation
- Market adjustment

Final price is displayed in SAR.

