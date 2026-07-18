#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量随机二进制文件生成器
支持多线程高效写入，支持大小单位解析

用法:
    python random_bin_generator.py -n 10 -m 1KB -M 10KB -o ./output
    python random_bin_generator.py --num-files 100 --minimum-size 100KB --maximum-size 1MB
"""

import os
import re
import random
import argparse
import string
from concurrent.futures import ThreadPoolExecutor


MAX_SIZE = 1024 ** 3

def parse_size(size_str):
    """解析大小字符串，如 '1024', '10KB', '5MB', '1GB' 等"""
    if size_str is None:
        return None
    size_str = str(size_str).strip().upper()
    if size_str.isdigit():
        return int(size_str)
    match = re.match(r'^(\d+(?:\.\d+)?)\s*(B|KB|MB|GB|TB)?$', size_str)
    if not match:
        raise ValueError(f"无法解析大小: {size_str}")
    value = float(match.group(1))
    unit = match.group(2) or 'B'
    multipliers = {'B': 1, 'KB': 1024, 'MB': 1024**2, 'GB': 1024**3}
    return int(value * multipliers[unit])


def human_readable(size):
    """将字节数转换为人类可读格式"""
    for unit in ['B', 'KB', 'MB', 'GB', 'TB']:
        if abs(size) < 1024:
            return f"{size:.2f} {unit}"
        size /= 1024
    return f"{size:.2f} PB"


def generate_random_filename(length=16):
    """生成随机文件名"""
    chars = string.ascii_letters + string.digits
    return ''.join(random.choices(chars, k=length)) + '.bin'


def generate_single_file(args_tuple):
    """生成单个随机二进制文件（用于多线程）"""
    output_dir, min_size, max_size, file_index, total_files = args_tuple

    # 随机决定文件大小
    file_size = random.randint(min_size, max_size)

    # 生成唯一文件名
    filename = f"random_{file_index:04d}_{generate_random_filename(8)}"
    filepath = os.path.join(output_dir, filename)

    # 使用 os.urandom 生成随机数据（比 random.randint 快得多，且是加密安全的）
    # 分块写入，避免大文件占用过多内存
    chunk_size = 4 * 1024 * 1024  # 4MB 块大小，平衡内存和IO效率

    with open(filepath, 'wb') as f:
        remaining = file_size
        while remaining > 0:
            current_chunk = min(chunk_size, remaining)
            f.write(os.urandom(current_chunk))
            remaining -= current_chunk

    actual_size = os.path.getsize(filepath)
    return {
        'index': file_index,
        'filename': filename,
        'filepath': filepath,
        'size': actual_size,
        'size_human': human_readable(actual_size)
    }


def generate_random_binary_files(num_files=1, min_size_str='1KB', max_size_str='1MB',
                                  output_dir='.', max_total_size=1024**3):
    """
    批量生成随机二进制文件

    参数:
        num_files: 文件数量 (1-1000)
        min_size_str: 最小文件大小
        max_size_str: 最大文件大小
        output_dir: 输出目录
        max_total_size: 总大小上限，默认1GB
    """
    # 参数校验
    if not (1 <= num_files <= 1000):
        raise ValueError(f"文件数量必须在 1-1000 之间，当前: {num_files}")

    min_size = parse_size(min_size_str)
    max_size = parse_size(max_size_str)

    if min_size < 0 or max_size < 0:
        raise ValueError("文件大小不能为负数")
    if min_size > max_size:
        raise ValueError(f"最小大小 ({min_size}) 不能大于最大大小 ({max_size})")
    if max_size > MAX_SIZE:
        raise ValueError(f"最大大小 ({max_size}) 超出 1GB 限制")

    # 确保输出目录存在
    os.makedirs(output_dir, exist_ok=True)

    # 估算总大小上限检查
    estimated_max_total = num_files * max_size
    if estimated_max_total > max_total_size:
        raise ValueError(
            f"估算最大总大小 {human_readable(estimated_max_total)} 超过限制 "
            f"{human_readable(max_total_size)}。请减少文件数量或降低单文件大小上限。"
        )

    print(f"🚀 开始生成 {num_files} 个随机二进制文件...")
    print(f"   大小范围: {human_readable(min_size)} ~ {human_readable(max_size)}")
    print(f"   输出目录: {os.path.abspath(output_dir)}")
    print('')

    # 准备任务参数
    task_args = [
        (output_dir, min_size, max_size, i + 1, num_files)
        for i in range(num_files)
    ]

    # 使用多线程并行生成文件（IO密集型任务，线程数可较高）
    if num_files <= 10:
        max_workers = num_files
    elif num_files <= 100:
        max_workers = min(20, num_files)
    else:
        max_workers = min(50, num_files)

    results = []
    total_bytes = 0

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        for result in executor.map(generate_single_file, task_args):
            results.append(result)
            total_bytes += result['size']
            # 每10%打印一次进度
            step = max(1, num_files // 10)
            if result['index'] % step == 0 or result['index'] == num_files:
                print(f"   📄 [{result['index']}/{num_files}] {result['filename']} ({result['size_human']})")

    print()
    print(f"✅ 全部完成！")
    print(f"   生成文件数: {len(results)}")
    print(f"   总大小: {human_readable(total_bytes)}")
    print(f"   平均大小: {human_readable(total_bytes // num_files)}")
    print(f"   输出目录: {os.path.abspath(output_dir)}")

    return results


def main():
    parser = argparse.ArgumentParser(
        description='批量生成随机二进制文件',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
使用示例:
  python random_bin_generator.py -n 10 -m 1KB -M 10KB -o ./output
  python random_bin_generator.py --num-files 100 --minimum-size 100KB --maximum-size 1MB
  python random_bin_generator.py -n 5 -m 1MB -M 5MB
        """
    )

    parser.add_argument('-n', '--num-files', type=int, default=1,
                        help='文件数量 (1-1000，默认: 1)')
    parser.add_argument('-m', '--minimum-size', type=str, default='1KB',
                        help='最小文件大小，支持 B/KB/MB/GB (默认: 1KB)')
    parser.add_argument('-M', '--maximum-size', type=str, default='1MB',
                        help='最大文件大小，支持 B/KB/MB/GB (默认: 1MB)')
    parser.add_argument('-o', '--output', type=str, default='.',
                        help='输出目录 (默认: 当前目录)')

    args = parser.parse_args()

    try:
        generate_random_binary_files(
            num_files=args.num_files,
            min_size_str=args.minimum_size,
            max_size_str=args.maximum_size,
            output_dir=args.output
        )
    except ValueError as e:
        print(f"❌ 错误: {e}")
        return 1
    except Exception as e:
        print(f"❌ 意外错误: {e}")
        return 1

    return 0


if __name__ == "__main__":
    main()
