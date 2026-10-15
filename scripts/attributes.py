import math
base=20
max_scale=20
power=1.8
offset=400
flatten=0.35
cap=1024
classes={'common':1.0,'uncommon':1.3,'rare':1.6,'epic':2.2,'legendary':3.0,'mythic':4.0}
levels=[1,10,20,30,40,50,70,100]
print('HP (capped at 1024)')
print('Lvl\t'+'\t'.join(classes.keys()))
for lvl in levels:
 lp=lvl**power
 prog=lp/(offset+lp)
 scale=1+(max_scale-1)*prog
 row=[lvl]
 for cm in classes.values():
  eff=1+(cm-1)*(1-flatten*prog)
  hp=min(cap, round(base*scale*eff,1))
  row.append(hp)
 print('\t'.join(map(str,row)))

# Damage
base_dmg=4  # vanilla zombie damage (hearts*2)
dmg_max_scale=6.0
dmg_power=1.2
dmg_offset=600
dmg_flatten=0.40
print('\nDamage (base 4)')
print('Lvl\t'+'\t'.join(classes.keys()))
for lvl in levels:
 lp=lvl**dmg_power
 prog=lp/(dmg_offset+lp)
 scale=1+(dmg_max_scale-1)*prog
 row=[lvl]
 for cm in classes.values():
  eff=1+(cm-1)*(1-dmg_flatten*prog)
  dmg=round(base_dmg*scale*eff,2)
  row.append(dmg)
 print('\t'.join(map(str,row)))