<?php
$watermark = 'Geeky Hallows Galaxy-png.png';

$w_size = `identify -format "%[fx:w]x%[fx:h]" "$watermark"`;
list($ww,$wh) = explode('x', $w_size);


$files = scandir('.');

foreach ($files as $file) {
	if ($file{0} === '.' || $watermark == $file || $file == 'watermark.php') continue;
	$size = `identify -format "%[fx:w]x%[fx:h]" $file`;
	list($x,$y) = explode('x', $size);

	$w_w = (int) ($x / 2);
	$w_h = (int) ($y / 2);
	$left = (int) ($x) - (int) ($w_w / 1.5);
	$top = -75; //(int) ($y) - (int) ($w_h);

	`composite -compose multiply -gravity SouthWest -geometry +5+5 "Geeky Hallows Galaxy-png.png" "$file" -geometry {$w_w}x{$w_h}+{$left}+{$top} watermarked/{$file}`;
}
