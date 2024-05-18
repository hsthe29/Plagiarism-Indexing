# Plagiarism Indexing

### Author: Sy The Ho - @hsthe29

<hr>

## Framework

1. Ktor
2. Elasticsearch

## REST API:
- GET: 
  - /: Server Information

- POST:
  - /index
    ```json
    {
        "directory_path": "D:\\CoopyData\\Test",
        "extract": false,
        "university_id": 1,
        "category": 1,
        "private": true,
        "language": "vi",
        "type": 0
    }
  - /search
    ```json
    {
        "num_files": 2,
        "keywords": [
            "transformer", "cnn", "huấn luyện", "encoder", "giám sát", "mô hình", "học sâu", "dáng người"
        ],
        "language": "vi"
    }
