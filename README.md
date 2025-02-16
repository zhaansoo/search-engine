# SearchEngine-Master

🚀 **SearchEngind-Master** is a Java-based text indexing and search system that extracts relevant snippets from stored HTML pages. It processes text, finds matches based on lemmatization, and highlights search terms in the extracted snippets.

## 📌 Features

✅ Extracts **relevant snippets** from indexed HTML pages  
✅ Uses **lemmatization** for accurate word matching  
✅ Highlights search terms in **bold (`<b>...<b>`)**  
✅ Sorts results by **relevance**  
✅ Ensures **uniform snippet length** for better readability  

## 🛠️ Technologies Used

- **Java 17+**
- **Jsoup** (for HTML parsing)
- **Spring Boot** 
- **Lemmatization Library** 
- **Maven** 
- **MySQL**
## Before Usage
--- **Run this command:**
- docker run --name mysql-container -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=root -p 3306:3306 -d mysql:latest


## 📂 How to Run Project
📂 /src/main/java/searchengine/Application.java
