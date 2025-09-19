import os
from PIL import Image, ImageEnhance
import shutil
from pathlib import Path
import numpy as np

def apply_yellow_filter(image):
    # Convert PIL Image to numpy array
    img_array = np.array(image)
    
    # Convert RGB to HSV color space
    img_hsv = np.copy(img_array)
    r, g, b = img_array[:,:,0], img_array[:,:,1], img_array[:,:,2]
    
    # Calculate HSV values
    maxc = np.maximum(np.maximum(r, g), b)
    minc = np.minimum(np.minimum(r, g), b)
    
    # Value (Brightness)
    v = maxc
    
    # Saturation
    s = np.zeros_like(v)
    mask = maxc != 0
    s[mask] = (maxc[mask] - minc[mask]) / maxc[mask]
    
    # Hue
    h = np.zeros_like(v, dtype=float)
    mask = (maxc != minc) & (maxc == r)
    h[mask] = 60 * ((g[mask] - b[mask]) / (maxc[mask] - minc[mask]))
    mask = (maxc != minc) & (maxc == g)
    h[mask] = 60 * (2 + (b[mask] - r[mask]) / (maxc[mask] - minc[mask]))
    mask = (maxc != minc) & (maxc == b)
    h[mask] = 60 * (4 + (r[mask] - g[mask]) / (maxc[mask] - minc[mask]))
    h[h < 0] += 360
    h = h % 360  # Ensure hue is in [0, 360) range
    
    # Create yellow mask (hue around 60 degrees, with good saturation)
    # Yellow in HSV is around 60 degrees
    yellow_mask = (
        ((h >= 45) & (h <= 75)) &  # Yellow hue range
        (s >= 0.15) &              # Minimum saturation to avoid grays
        (v >= 50)                  # Minimum brightness
    )
    
    # Create a weight matrix for smooth enhancement
    weight = np.zeros_like(h)
    yellow_range = 15  # Degrees from center of yellow (60)
    weight[yellow_mask] = np.clip(1 - np.abs(h[yellow_mask] - 60) / yellow_range, 0, 1)
    
    # Enhance yellow regions
    enhancement = 1.35  # 35% enhancement
    for i in range(3):  # Apply to all RGB channels
        img_array[:,:,i] = np.clip(
            img_array[:,:,i] * (1 + (enhancement - 1) * weight),
            0, 255
        ).astype(np.uint8)
    
    # Additional saturation boost for yellow regions
    img_array[yellow_mask] = np.clip(
        img_array[yellow_mask] * 1.15,  # 15% extra boost
        0, 255
    ).astype(np.uint8)
    
    # Convert back to PIL Image
    return Image.fromarray(img_array)

def apply_adjustments(image, brightness_factor, contrast_factor):
    # Apply brightness adjustment
    enhancer = ImageEnhance.Brightness(image)
    image = enhancer.enhance(brightness_factor)
    
    # Apply contrast adjustment
    enhancer = ImageEnhance.Contrast(image)
    image = enhancer.enhance(contrast_factor)
    
    # Always apply yellow highlighting
    image = apply_yellow_filter(image)
    
    return image

def process_images():
    # Input and output directories
    input_dir = "images/test"
    output_dir = "outputs/cache/processed"
    
    # Create output directory if it doesn't exist
    Path(output_dir).mkdir(parents=True, exist_ok=True)
    
    # Variations for brightness and contrast (±25%)
    variations = [
        {"brightness": 1.25, "contrast": 1.25},  # +25% brightness, +25% contrast
        {"brightness": 1.25, "contrast": 0.75},  # +25% brightness, -25% contrast
        {"brightness": 0.75, "contrast": 1.25},  # -25% brightness, +25% contrast
        {"brightness": 0.75, "contrast": 0.75},  # -25% brightness, -25% contrast
    ]
    
    # Process each image in the input directory
    for filename in os.listdir(input_dir):
        if filename.lower().endswith(('.png', '.jpg', '.jpeg')):
            input_path = os.path.join(input_dir, filename)
            
            try:
                # Open the image
                with Image.open(input_path) as img:
                    # Convert image to RGB if it's not
                    if img.mode != 'RGB':
                        img = img.convert('RGB')
                    
                    # Apply each variation
                    for i, var in enumerate(variations):
                        # Apply adjustments
                        processed_img = apply_adjustments(
                            img.copy(),
                            var["brightness"],
                            var["contrast"]
                        )
                        
                        # Generate output filename
                        name, ext = os.path.splitext(filename)
                        brightness_str = f"_b{'+' if var['brightness'] > 1 else '-'}25"
                        contrast_str = f"_c{'+' if var['contrast'] > 1 else '-'}25"
                        output_filename = f"{name}{brightness_str}{contrast_str}{ext}"
                        output_path = os.path.join(output_dir, output_filename)
                        
                        # Save the processed image
                        processed_img.save(output_path)
                        print(f"Processed: {output_filename}")
                        
            except Exception as e:
                print(f"Error processing {filename}: {str(e)}")

if __name__ == "__main__":
    process_images()
