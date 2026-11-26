import sys, struct
# usage: pix.py file.bmp x y  -> prints r g b  (24/32-bit BMP from sips)
f=open(sys.argv[1],'rb').read()
off=struct.unpack_from('<I',f,10)[0]; w,h=struct.unpack_from('<ii',f,18); bpp=struct.unpack_from('<H',f,28)[0]
x,y=int(sys.argv[2]),int(sys.argv[3]); bpr=((w*bpp+31)//32)*4
row=(h-1-y) if h>0 else y
p=off+row*bpr+x*(bpp//8)
b,g,r=f[p],f[p+1],f[p+2]; print(r,g,b)
