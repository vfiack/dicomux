# Dicomux
...is a viewer for DICOM ECG. 

Waveforms and encapsulated PDFs are supported. 


## NO WARRANTY
THE PROGRAM IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL, BUT WITHOUT ANY WARRANTY. 
IT IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, 
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
FOR A PARTICULAR PURPOSE.

IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW THE AUTHORS WILL BE LIABLE TO YOU FOR 
DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING 
OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA 
OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE
OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS). 

### Licenses
This software is licensed under the terms of the <a href="http://www.gnu.org/licenses/gpl-3.0.html">GNU General Public License Version 3</a> as published by the <a href="http://www.fsf.org/">Free Software Foundation</a>.

Thomas Heidrich created the logo of Dicomux by deriving from <a href="http://commons.wikimedia.org/wiki/File:Linia_izoelektryczna_EKG.svg">Mrug's work</a>. The logo of Dicomux is licensed under the terms of <a href="http://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA 3.0</a>.

### Compiling & Running
Dicomux is a Java Application. No binary is provided.
You can compile with maven : mvn package assembly:assembly
Then run the generated jar in the target directory : java -jar dicomux*with-dependencies.jar

### Logo
<img src="raw/master/src/main/resources/images/logo_big.png" alt="Dicomux Logo" width="30%"/>

### Used external libraries
<table>
 <tr>
  <th>Name</th><th>License</th><th>URL</th>
 </tr>
 <tr>
  <td>dcm4che</td><td>MPLv1.1, GPLv2, LGPLv2.1</td><td><a href="http://www.dcm4che.org/">dcm4che.org</a></td>
 </tr>
 <tr>
  <td>pdf-renderer</td><td>LGPL</td><td><a href="https://pdf-renderer.dev.java.net/">pdf-renderer.dev.java.net</a></td>
 </tr>
 <tr>
  <td>jai-imageio</td><td>BSD 3-clause</td><td><a href="https://jai-imageio.dev.java.net/">jai-imageio.dev.java.net</a></td>
 </tr>
</table>

