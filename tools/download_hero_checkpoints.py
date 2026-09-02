import os
import sys
import time
import zipfile
import urllib.request
import shutil

ANIME_URLS = {
    "face_paint_512_v2.pt": "https://github.com/bryandlee/animegan2-pytorch/raw/main/weights/face_paint_512_v2.pt",
    "face_paint_512_v1.pt": "https://github.com/bryandlee/animegan2-pytorch/raw/main/weights/face_paint_512_v1.pt",
    "paprika.pt": "https://github.com/bryandlee/animegan2-pytorch/raw/main/weights/paprika.pt",
    "celeba_distill.pt": "https://github.com/bryandlee/animegan2-pytorch/raw/main/weights/celeba_distill.pt"
}

DROPBOX_FAST_STYLE_URL = "https://www.dropbox.com/s/lrvwfehqdcxoza8/saved_models.zip?dl=1"

def download_file_with_retry(url: str, dest_path: str, max_retries: int = 4):
    os.makedirs(os.path.dirname(dest_path), exist_ok=True)
    if os.path.exists(dest_path) and os.path.getsize(dest_path) > 1024:
        print(f"File already exists: {dest_path} ({os.path.getsize(dest_path)} bytes)")
        return True

    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    
    for attempt in range(1, max_retries + 1):
        try:
            print(f"Downloading {url} (Attempt {attempt}/{max_retries})...")
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=30) as response, open(dest_path, "wb") as out_file:
                shutil.copyfileobj(response, out_file)
            size = os.path.getsize(dest_path)
            print(f"Successfully downloaded {dest_path} ({size / (1024*1024):.2f} MB)")
            return True
        except Exception as e:
            print(f"Download failed: {e}")
            if os.path.exists(dest_path):
                os.remove(dest_path)
            if attempt < max_retries:
                time.sleep(2 * attempt)
    return False

def main():
    base_dir = "tools/checkpoints"
    anime_dir = os.path.join(base_dir, "anime")
    fine_art_dir = os.path.join(base_dir, "fine_art")

    os.makedirs(anime_dir, exist_ok=True)
    os.makedirs(fine_art_dir, exist_ok=True)

    print("=== 1. Downloading AnimeGANv2 Hero Checkpoints ===")
    for filename, url in ANIME_URLS.items():
        dest = os.path.join(anime_dir, filename)
        success = download_file_with_retry(url, dest)
        if not success:
            print(f"Warning: Could not download {filename}")

    print("\n=== 2. Downloading PyTorch Fast-Neural-Style Archive ===")
    zip_dest = os.path.join(base_dir, "saved_models.zip")
    success = download_file_with_retry(DROPBOX_FAST_STYLE_URL, zip_dest)
    if success:
        print("Extracting saved_models.zip...")
        extract_tmp = os.path.join(base_dir, "tmp_extracted")
        with zipfile.ZipFile(zip_dest, "r") as zip_ref:
            zip_ref.extractall(extract_tmp)

        # Move extracted .pth files to fine_art/
        found_models = []
        for root, _, files in os.walk(extract_tmp):
            for file in files:
                if file.endswith(".pth"):
                    src_file = os.path.join(root, file)
                    dest_file = os.path.join(fine_art_dir, file)
                    shutil.copy2(src_file, dest_file)
                    found_models.append(file)
                    print(f"Ingested {file} into {fine_art_dir}")

        shutil.rmtree(extract_tmp, ignore_errors=True)
        print(f"Extracted models: {found_models}")

    print("\nCheckpoints download process complete.")

if __name__ == "__main__":
    main()
