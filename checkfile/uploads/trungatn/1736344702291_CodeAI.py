import tkinter as tk
from tkinter import Listbox, messagebox
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt 
import seaborn as sns


class DungTichHoSoApp:
    def __init__(self):
        # tạo ra tab window tên là frame
        self.frame = tk.Tk()
        # tên tiêu đề của tab
        self.frame.title("Dự đoán chiều cao qua dung tích hộp số")
        # tỉ lệ khung hình của tab
        self.frame.geometry("800x450")
        # khai báo thư viện làm mẫu
        self.library = {
            1: {"name": "Australopithecus Afarensis"},
            2: {"name": "Homo Floresiensis"},
            3: {"name": "Paranthropus Boisei"},

        }

        # Nhãn GUI
        self.label_select = tk.Label(self.frame, text="Dự đoán chiều cao qua dung tích hộp ", font=("Helvetica", 12))
        self.label_select.grid(row=0, column=0, columnspan=4)

        # Nhãn chọn giống loài
        self.label_select = tk.Label(self.frame, text="Vui lòng chọn giống loài để lấy dữ liệu", font=("Helvetica", 12))
        self.label_select.grid(row=1, column=0)

        # Listbox chọn giống loài
        self.listbox = Listbox(self.frame, width=45, height=15, selectmode="SINGLE", exportselection=0, font=("Helvetica", 12))
        self.listbox.grid(row=2, column=0, rowspan=6)
        # cập nhật listbox để hiện ra những tên ví dụ trong thư viện mà đã khai báo ở trên
        self.populate_listbox()
        # bind(thêm) vào listbox <<listboxselect>> để nhận những hành động (event) khi người dùng ấn chuột trái 1 lần vào listbox (mouse-1) 
        self.listbox.bind("<<ListboxSelect>>", self.on_select) # tuy nhiên cái <<ListboxSelect>> này nhanh hơn <<Mouse-1>> nên là dùng cái này thay vì cái <<mouse-1>>

        # Dung tích lớn nhất
        self.label_dungtich_lon_nhat = tk.Label(self.frame, text="Dung tích sọ lớn nhất", font=("Helvetica", 12))
        self.label_dungtich_lon_nhat.grid(row=1, column=1)
        self.entry_dungtich_lon_nhat = tk.Entry(self.frame, width=25)
        self.entry_dungtich_lon_nhat.grid(row=1, column=2, columnspan=2)

        # Dung tích nhỏ nhất
        self.label_dungtich_nho_nhat = tk.Label(self.frame, text="Dung tích sợ nhỏ nhất", font=("Helvetica", 12))
        self.label_dungtich_nho_nhat.grid(row=2, column=1)
        self.entry_dungtich_nho_nhat = tk.Entry(self.frame, width=25)
        self.entry_dungtich_nho_nhat.grid(row=2, column=2, columnspan=2)

        # Chiều cao lớn nhất
        self.label_chieucao_lon_nhat = tk.Label(self.frame, text="Chiều cao lớn nhất", font=("Helvetica", 12))
        self.label_chieucao_lon_nhat.grid(row=3, column=1)
        self.entry_chieucao_lon_nhat = tk.Entry(self.frame, width=25)
        self.entry_chieucao_lon_nhat.grid(row=3, column=2, columnspan=2)

        # Chiều cao nhỏ nhất
        self.label_chieucao_nho_nhat = tk.Label(self.frame, text="Chiều cao nhỏ nhất", font=("Helvetica", 12))
        self.label_chieucao_nho_nhat.grid(row=4, column=1)
        self.entry_chieucao_nho_nhat = tk.Entry(self.frame, width=25)
        self.entry_chieucao_nho_nhat.grid(row=4, column=2, columnspan=2)

        # B
        self.label_b = tk.Label(self.frame, text="B =", font=("Helvetica", 12))
        self.label_b.grid(row=5, column=1)
        self.entry_b = tk.Entry(self.frame, width=25)
        self.entry_b.grid(row=5, column=2, columnspan=2)

        # e
        self.label_e = tk.Label(self.frame, text="e =", font=("Helvetica", 12))
        self.label_e.grid(row=6, column=1)
        self.entry_e = tk.Entry(self.frame, width=25)
        self.entry_e.grid(row=6, column=2, columnspan=2)

        # r2
        self.label_r2 = tk.Label(self.frame, text="r2 =", font=("Helvetica", 12))
        self.label_r2.grid(row=7, column=1)
        self.entry_r2 = tk.Entry(self.frame, width=25)
        self.entry_r2.grid(row=7, column=2, columnspan=2)

        # blank
        self.label_r2 = tk.Label(self.frame, text="", font=("Helvetica", 12))
        self.label_r2.grid(row=8, column=0, columnspan=4)

        # Nhãn và ô nhập dung tích hộp số
        self.label_hopso = tk.Label(self.frame, text="nhập dung tích hộp số", font=("Helvetica", 12))
        self.label_hopso.grid(row=9, column=0)

        self.entry_hopso = tk.Entry(self.frame, width=25)
        self.entry_hopso.grid(row=9, column=1, columnspan=2)

        # Nút tính
        self.submit_btn = tk.Button(self.frame, text="Tính dự đoán", width=15, command=self.tinh_chieu_cao)
        self.submit_btn.grid(row=9, column=3, sticky="E")

        # Nhãn định dạng hộp số
        self.label_format = tk.Label(self.frame, text="(min-max)", font=("Helvetica", 12))
        self.label_format.grid(row=10, column=1, columnspan=3)

        self.frame.mainloop()


    # truy cập vào dictionary để hiện ra giống loài ở listbox(vùng hiển thị)
    def populate_listbox(self):
        for id, info in self.library.items():
            self.listbox.insert(tk.END, f"{id}. {info['name']}")

    # để lấy dữ liệu khi người dùng chọn vào listbox
    def on_select(self, event):
        self.selected_index = self.listbox.curselection() #trả về id mà current selection. trả về (0,) nếu đang chọn dòng 0 (vì index bắt đầu từ 0 cho dòng số 1)
        if not self.selected_index:
            messagebox.showerror("Lỗi", "Lỗi: bạn đang không chọn gì cả")
            return
        clear_selected_index = self.selected_index[0] # xử lí từ dạng (0,) -> 0
        selected_item = self.listbox.get(clear_selected_index) #lấy dữ liệu từ index 0 (dòng 1) => trả về "1. tên giống loài"
        split_by_dot = selected_item.split('.')  # tách chuối bởi dấu . và trả về + lưu theo kiểu array (mảng)
        genus_id = int(split_by_dot[0]) # Lấy ID từ chuỗi tại index 0

        # Kiểm tra xem ID có tồn tại trong thư viện không
        if genus_id  not in self.library:   
            messagebox.showerror("Lỗi", "Lỗi: không tìm thấy giống loài")
            return
        genus = self.library[genus_id]["name"] #lấy tên giống loài qua id đã có
        # sau khi có tên giống loài, thì mình sẽ tính df qua nó
        self.calculate_data_frame(genus)


    # cập nhật ô hiện dữ liệu entry
    def update_entry(self, entry, value):
        entry.config(state="normal") #chuyển trạng thái thành mở để sửa
        entry.delete(0, tk.END)
        entry.insert(0, f"{value:.2f}")
        entry.config(state="readonly") #chuyển trạng thái thành "chỉ đọc" để tránh người dùng nhập giá trị sai

    # tính Data frame dựa vào sơ sở dữ liệu (database)
    def calculate_data_frame(self, genus_name):
        data = pd.read_csv('tienhoa.csv', skiprows = 0, nrows = 20000)
        pd.set_option('display.max_columns', None)  
        pd.set_option('display.max_rows', None)    

        df1 = data[data['Giong loai'] == genus_name]
        # kiểm tra nếu là rỗng thì báo lỗi và thoát hàm
        if df1.empty:
            messagebox.showerror("lỗi", f" không tìm thấy dữ liệu theo giống loài: {genus_name}")
            return 
        # sau khi có được df, thì mình tính được Dung tích hộp sọ, chiều cao, B, E.
        self.calculate_cranical_and_height(df1)
        self.calculate_B_and_E(df1)

    # tính dung tích hộp sọ và chiều cao
    def calculate_cranical_and_height(self, df1):
        self.maxcranical = df1['Dung tich hop so'].max(axis= 0) #global var
        self.mincranical = df1['Dung tich hop so'].min(axis= 0) #global var
        minheight = df1['Chieu cao'].min(axis= 0)   #local var
        maxheight = df1['Chieu cao'].max(axis= 0)   #local var
        self.label_format.config(text=f"vui lòng nhập trong khoảng:\n ({self.mincranical:.2f}-{self.maxcranical:.2f})")
        
        # cập nhật để hiện dữ liệu
        self.update_entry(self.entry_dungtich_lon_nhat, self.maxcranical)
        self.update_entry(self.entry_dungtich_nho_nhat, self.mincranical)
        self.update_entry(self.entry_chieucao_lon_nhat, maxheight)
        self.update_entry(self.entry_chieucao_nho_nhat, minheight)

    # Tính weight(cân nặng) và bias
    def calculate_B_and_E(self, df1):
        self.x = df1['Dung tich hop so'].values
        self.y = df1['Chieu cao'].values
        y_mean = np.mean(self.y)
        x_mean = np.mean(self.x)

        n = len(self.x)
        atemp = 0
        btemp = 0 
        for i in range(n):
            atemp += (self.x[i] - x_mean) * (self.y[i] - y_mean)
            btemp += (self.x[i] - x_mean) ** 2
        self.weight = atemp / btemp
        self.bias = y_mean - (self.weight * x_mean)

        # cập nhật để hiện dữ liệu
        self.update_entry(self.entry_b, self.weight)
        self.update_entry(self.entry_e, self.bias)
        #sau khi có được kết quả của B và E, thì mình tính được R2
        self.calculate_r2(self.weight, self.x, self.y, self.bias, y_mean)

    # tính R2
    def calculate_r2(self, weight, x, y, bias, y_mean):
        sst, ssr = 0.0, 0.0
        n = len(x)
        for i in range(n):
            ypred = weight * x[i] + bias 
            sst += (y[i] - y_mean) ** 2
            ssr += (y[i] - ypred ) ** 2
        r2 = 1 - ssr/sst 
        # cập nhật để hiện dữ liệu
        self.update_entry(self.entry_r2, r2)

    # thực hiện một số hành động khi người dùng ấn "BUTTON"
    def tinh_chieu_cao(self):
        try:
            self.input_cranical = float(self.entry_hopso.get().strip()) # get() để lấy dữ liệu nhập vào và strip() để loại bỏ những khoảng trống không muốn
            if not (self.mincranical < self.input_cranical < self.maxcranical):  # kiểm tra xem có nhập đúng trong khoảng cho phép hay không (nếu sai thì thoát hàm, nếu đúng thì chạy tiếp cái ở dưới là ve_bieu_do)
                messagebox.showerror("Lỗi", "Vui lòng nhập đúng giá trị cho phép")
                return
            self.chieu_cao_du_doan()
            self.ve_bieu_do()

        except ValueError:
            messagebox.showerror("Lỗi", "Vui lòng nhập giá trị là số ")

    # tính chiều cao để hiện thông báo kết quả
    def chieu_cao_du_doan(self):
        chieu_cao_du_doan = f'{self.input_cranical * self.weight + self.bias} cm'
        messagebox.showinfo("Thông báo", f"Chiều cao dự đoán: {chieu_cao_du_doan}")

    # vẽ biểu đồ bằng matplotlib
    def ve_bieu_do(self):
        x_min = np.min(self.x)
        x_max = np.max(self.x)
        y_min = self.weight * x_min + self.bias
        y_max = self.weight * x_max + self.bias

        plt.figure(figsize=(15,5))
        sns.lineplot(
            x = [x_min , x_max],
            y = [y_min , y_max],
            linewidth = 1,
            color = 'red',
            label = 'dt tuyen tinh'
        )
        plt.xlabel('dung tich hop so (ml)')
        plt.ylabel('chieu cao (cm)')
        plt.scatter(self.x, self.y, c = 'blue', label = 'do thi ')
        plt.legend()
        plt.show()


# tạo ra class để chạy
app = DungTichHoSoApp()
