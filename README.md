# INA-WikiMinerCoreference
INA-WikiMinerCoreference adalah sebuah kakas *Coreference Resolution* untuk teks Berbahasa Indonesia dengan pendekatan basis pengetahuan eksternal. Kakas ini dikembangkan dengan memanfaatkan kakas *entity linking* [WikipediaMiner](https://github.com/dnmilne/wikipediaminer) untuk diadaptasikan terhadap persoalan *coreference resolution*. Kakas INA-WikiMinerCoreference adalah hasil dari penelitian tugas akhir Teknik Informatika ITB pada tahun 2016. Makalah tugas akhir ini dapat dilihat di [sini](https://drive.google.com/file/d/0B4i1HUP9D46bTDZfdW51WXZUclE/view?usp=sharing).

Pendekatan basis pengetahuan yang dikembangkan pada kakas *coreference resolution* kali ini berhasil meningkatkan kinerja sebesar 6.00% CEAFm F-measure. Pengingkatan ini adalah hasil dari pemanfaatan pengetahuan semantik yang didapat dari basis pengetahuan Wikipedia yang dapat menangani persoalan *coreference resolution* terutama pada jenis variasi *proper noun*.

## Direktori penting
* `/annotationWorkbench` : model dan data latih untuk *disambiguation*
* `/configs` : konfigurasi bahasa untuk WikipediaMiner
* `/data` : data percobaan INA-WikiMinerCoreference
* `/db` : basis data *entity linking* Wikipedia Indonesia, dapat diunduh di [sini](https://drive.google.com/drive/folders/0B4i1HUP9D46bcjRtTkZhb3Y0eG8?usp=sharing) (size : 3.61 GB).
* `/lib` : kakas-kakas tambahan sebagai *library*
* `/src` : *source code*
* `/WikiMinerCoreference.jar` : *executable file* untuk demo

## Modul
Terdapat beberapa modul dalam project tugas akhir kali ini, antara lain:

1. `arcoref` : *baseline project* (Indra Budi dkk., 2006)
2. `helper` : modul pembantu yang meliputi *chain reader/writer*, *constants*, *corpus generator*, serta penghitung kinerja
3. `wikicoref` : modul utama dari tugas akhir yang dikerjakan
4. `wikiminer` : modul untuk percobaan WikipediaMiner

## Cara menjalankan program demo
1. Pastikan pada direktori `/data/corefdata/demo/` minimal terdapat *file* `raw.txt` dan `key.xml` (isi *file* ini dapat diubah sesuai contoh kasus uji yang ingin dicoba)
2. Buka command prompt pada root directory project
3. Masukkan perintah `java -jar "WikiMinerCoreference.jar"`
4. Program akan melakukan *load database* dan selanjutnya akan membaca text yang terdapat pada `raw.txt` dan membaca *key chain* pada `key.xml` untuk kemudian dilakukan proses *coreference resolution* dan penghitungan nilai kinerja F-measure dengan standar CEAFm.

&copy; 2016: [Rakhmatullah Yoga Sutrisna](http://github.com/rakhmatullahyoga)
