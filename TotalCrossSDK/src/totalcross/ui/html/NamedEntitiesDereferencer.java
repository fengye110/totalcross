/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2003-2004 Pierre G. Richard                                    *
 *  Copyright (C) 2003-2011 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *  This file is covered by the GNU LESSER GENERAL PUBLIC LICENSE VERSION 3.0    *
 *  A copy of this license is located in file license.txt at the root of this    *
 *  SDK or can be downloaded here:                                               *
 *  http://www.gnu.org/licenses/lgpl-3.0.txt                                     *
 *                                                                               *
 *********************************************************************************/



package totalcross.ui.html;

/**
* <code>NamedEntitiesDereferencer</code> provides an extremely fast way
* to map Named Entities References to their corresponding Unicode value.
* <P>
* <b>Note:</b> the Entity table described below comes from the official XHTML
*       entity reference list.&nbsp;  Also, notice that entity names are
*       case sensitive.
*/

public class NamedEntitiesDereferencer
{
   /* This table was generated by Jaxo's GenStatTable utility - do not edit! */
   /** GenStatTable double-hash list
   *    - input file: file:/D:/u/pgr/com/jaxo/html/HtmlHRefProps.txt
   *    - gen date:   Aug 31, 2003 6:49:55 PM CEST
   *    - 0 error(s), 0 warning(s)
   */
   private static final int entries[][] =
   {
      {
         -1824316352, 0x160,  // Scaron
         -1304807232, 0xe8,   // egrave
         -1105492192, 0x230a, // lfloor
         -908183520, 0x161,   // scaron
         114240, 0x2282,      // sub
         3433440, 0xb6,       // para
         70470880, 0xce,      // Icirc
         100023552, 0xee,     // icirc
         104663296, 0x96,     // ndash
         2074027232, 0xc8     // Egrave
      },{
         -1074341375, 0xb7,   // middot
         -902468895, 0x3c2,   // sigmaf
         76305729, 0x3a9,     // Omega
         105858401, 0x3c9     // omega
      },{
         -1366144414, 0xe7,   // ccedil
         119522, 0xa5,        // yen
         95576866, 0x2666,    // diams
         2012690050, 0xc7     // Ccedil
      },{
         -1425292669, 0xe1,   // aacute
         3555, 0x2228,        // or
         3343619, 0xaf,       // macr
         3449699, 0x221d,     // prop
         3526211, 0xa7,       // sect
         3541923, 0xb9,       // sup1
         105832675, 0x203e,   // oline
         109946563, 0xdf,     // szlig
         1953541795, 0xc1     // Aacute
      },{
         -1419323836, 0xe0,   // agrave
         -1220935388, 0x2026, // hellip
         67716, 0x3a7,        // Chi
         98468, 0x3c7,        // chi
         113860, 0xad,        // shy
         3541924, 0xb2,       // sup2
         92646980, 0xb4,      // acute
         103278564, 0x2018,   // lsquo
         103890628, 0xb5,     // micro
         104574660, 0x2207,   // nabla
         110621028, 0x2122,   // trade
         1959510628, 0xc0     // Agrave
      },{
         2469413, 0xd6,       // Ouml
         3059493, 0x2245,     // cong
         3117317, 0x2003,     // emsp
         3419557, 0xaa,       // ordf
         3422725, 0xf6,       // ouml
         3541541, 0x2286,     // sube
         3541925, 0xb3,       // sup3
         79882757, 0x3a3,     // Sigma
         94536933, 0xb8,      // cedil
         100097093, 0xa1,     // iexcl
         109435429, 0x3c3     // sigma
      },{
         80550, 0x3a8,        // Psi
         99334, 0xb0,         // deg
         111302, 0x3c8,       // psi
         3118278, 0x2002,     // ensp
         62191590, 0xc6,      // AElig
         76012006, 0xd4,      // Ocirc
         92697574, 0xe6,      // aelig
         100348102, 0x221e,   // infin
         105564678, 0xf4,     // ocirc
         110364486, 0xd7      // times
      },{
         -2026225689, 0x39b,  // Lambda
         -1110092857, 0x3bb,  // lambda
         2535, 0x39d,         // Nu
         3527, 0x3bd,         // nu
         107431, 0x200e,      // lrm
         68567943, 0x393,     // Gamma
         98120615, 0x3b3      // gamma
      },{
         -791593944, 0x2118,  // weierp
         2504, 0x39c,         // Mu
         3496, 0x3bc,         // mu
         83848, 0x3a4,        // Tau
         114600, 0x3c4,       // tau
         3049896, 0xa2,       // cent
         3492904, 0x232a      // rang
      },{
         -1922900887, 0xd5,   // Otilde
         -1006768055, 0xf5,   // otilde
         93596489, 0x201e,    // bdquo
         105954953, 0x2295,   // oplus
         1115315049, 0x3d1    // thetasym
      },{
         -1951530038, 0xd1,   // Ntilde
         -1535503510, 0x3b5,  // epsilon
         -1339299958, 0x2020, // dagger
         -1035397206, 0xf1,   // ntilde
         93127274, 0x2248,    // asymp
         108819690, 0x2019,   // rsquo
         129149770, 0x395,    // Epsilon
         2039534506, 0x2021   // Dagger
      },{
         -1654325877, 0xdd,   // Yacute
         -922037109, 0x203a,  // rsaquo
         -738193045, 0xfd,    // yacute
         114251, 0x2211,      // sum
         2290667, 0xcf,       // Iuml
         3243979, 0xef,       // iuml
         72265003, 0x39a,     // Kappa
         101817675, 0x3ba     // kappa
      },{
         3419564, 0xba,       // ordm
         3551660, 0x21d1,     // uArr
         3582412, 0x2191,     // uarr
         81553132, 0xdb,      // Ucirc
         106857100, 0xa3,     // pound
         111105804, 0xfb      // ucirc
      },{
         -1380416467, 0xa6,   // brvbar
         -1221256979, 0x2665, // hearts
         121037, 0x200d,      // zwj
         3124973, 0x20ac,     // euro
         96634189, 0x2205     // empty
      },{
         -1923745138, 0xd8,   // Oslash
         -1096866130, 0x2217, // lowast
         -1007612306, 0xf8,   // oslash
         -896191506, 0x2660,  // spades
         114254, 0x2283,      // sup
         3314158, 0x2329,     // lang
         97692206, 0x2044     // frasl
      },{
         -1768842481, 0xda,   // Uacute
         -1266526065, 0xbd,   // frac12
         -1266526001, 0xbe,   // frac34
         -852709649, 0xfa,    // uacute
         104431, 0x222b,      // int
         2171503, 0xcb,       // Euml
         3124815, 0xeb,       // euml
         3241935, 0x2208,     // isin
         3462287, 0x21d2,     // rArr
         3493039, 0x2192,     // rarr
         108270575, 0x221a    // radic
      },{
         -1762873648, 0xd9,   // Ugrave
         -874817968, 0x2234,  // there4
         -846740816, 0xf9,    // ugrave
         2066960, 0x392,      // Beta
         3020272, 0x3b2,      // beta
         103901296, 0x2212    // minus
      },{
         -1266526063, 0xbc,   // frac14
         -1093812015, 0x2039, // lsaquo
         2833, 0x39e,         // Xi
         3825, 0x3be,         // xi
         80209, 0x3a6,        // Phi
         110961, 0x3c6,       // phi
         3374865, 0xa0,       // nbsp
         63529457, 0xc5,      // Aring
         93082129, 0xe5,      // aring
         102790001, 0x2308    // lceil
      },{
         70002, 0x397,        // Eta
         98258, 0x2229,       // cap
         100754, 0x3b7,       // eta
         3391250, 0x2284,     // nsub
         94921618, 0x21b5     // crarr
      },{
         -991722477, 0x2030,  // permil
         109267, 0xac,        // not
         113011, 0x200f,      // rlm
         2052339, 0xc4,       // Auml
         2285107, 0x399,      // Iota
         3000915, 0x27,       // apos
         3005651, 0xe4,       // auml
         3035411, 0x2022,     // bull
         3238419, 0x3b9,      // iota
         3433459, 0x2202,     // part
         3437299, 0x22a5,     // perp
         102831699, 0x201c    // ldquo
      },{
         112788, 0xae,        // reg
         115924, 0xa8,        // uml
         75120884, 0x152,     // OElig
         105626868, 0x153     // oelig
      },{
         -1940617387, 0xd3,   // Oacute
         -1024484555, 0xf3,   // oacute
         3059573, 0xa9,       // copy
         3283541, 0x21d0,     // lArr
         3314293, 0x2190      // larr
      },{
         -1934648554, 0xd2,   // Ograve
         -1018515722, 0xf2,   // ograve
         -874702154, 0x2009,  // thinsp
         3525622, 0x22c5,     // sdot
         96757814, 0x2261,    // equiv
         102742326, 0xab      // laquo
      },{
         -1407576169, 0xe3,   // atilde
         -1006767049, 0x2297, // otimes
         3511, 0x2260,        // ne
         96727, 0x2227,       // and
         107351, 0x25ca,      // loz
         113879, 0x223c,      // sim
         3053847, 0x2c6,      // circ
         3449687, 0x220f,     // prod
         3541975, 0x2287,     // supe
         79799255, 0xde,      // THORN
         96955127, 0x2203,    // exist
         108331127, 0x2309,   // rceil
         110337015, 0xfe,     // thorn
         111502423, 0x3d2,    // upsih
         1971258295, 0xc3     // Atilde
      },{
         -1268790216, 0x2200, // forall
         2781944, 0x396,      // Zeta
         3735256, 0x3b6,      // zeta
         63082712, 0xc2,      // Acirc
         65915800, 0x394,     // Delta
         92635384, 0xe2,      // acirc
         95468472, 0x3b4,     // delta
         105008952, 0x2209,   // notin
         110363480, 0x2dc     // tilde
      },{
         -1345696935, 0x3bf,  // omicron
         -1331463047, 0xf7,   // divide
         -1180962279, 0xbf,   // iquest
         2585, 0x3a0,         // Pi
         3449, 0x2264,        // le
         3577, 0x3c0,         // pi
         68985, 0xd0,         // ETH
         82137, 0x3a1,        // Rho
         100761, 0xf0,        // eth
         112889, 0x3c1,       // rho
         3164377, 0x21d4,     // hArr
         3195129, 0x2194,     // harr
         3752377, 0x200c,     // zwnj
         108372825, 0x201d,   // rdquo
         318956345, 0x39f     // Omicron
      },{
         -933717286, 0x230b,  // rfloor
         -220346502, 0x3c5,   // upsilon
         96730, 0x2220,       // ang
         1444306778, 0x3a5    // Upsilon
      },{
         -2112392293, 0xcd,   // Iacute
         -1349120421, 0xa4,   // curren
         -1196259461, 0xed,   // iacute
         -985162565, 0xb1,    // plusmn
         -918079173, 0x2135,  // alefsym
         3515, 0x220b,        // ni
         2767323, 0x178,      // Yuml
         3720635, 0xff,       // yuml
         100313435, 0x2111    // image
      },{
         -2106423460, 0xcc,   // Igrave
         -1190290628, 0xec,   // igrave
         66776796, 0xca,      // Ecirc
         96329468, 0xea,      // ecirc
         108283452, 0xbb,     // raquo
         109236764, 0x201a    // sbquo
      },{
         111005, 0x3d6,       // piv
         3045213, 0x21d3,     // dArr
         3075965, 0x2193,     // darr
         94761597, 0x2663     // clubs
      },{
         3294, 0x2265,        // ge
         98878, 0x222a,       // cup
         3496350, 0x211c,     // real
         63357246, 0x391,     // Alpha
         80774782, 0x398,     // Theta
         92909918, 0x3b1,     // alpha
         110327454, 0x3b8     // theta
      },{
         -1310776065, 0xe9,   // eacute
         2648159, 0xdc,       // Uuml
         3147935, 0x192,      // fnof
         3601471, 0xfc,       // uuml
         77382239, 0x2033,    // Prime
         103739775, 0x97,     // mdash
         106934911, 0x2032,   // prime
         2068058399, 0xc9     // Eacute
      }
   };

   /**
   * Get the code associated to a key.
   *
   * @param b byte array containing the key
   * @param offset position of the first byte of the key in the array
   * @param count number of bytes composing the key
   * @return the corresponding character value, or 0 if invalid
   */
   public static char toCode(byte b[], int offset, int count)
   {
      int key = 0;
      // compute the key associated to the series of bytes
      while (count-- > 0)
      {
         byte ch = b[offset++];
         key = (key << 5) - key + ch;
      }
      int[] bucket = entries[key & 0x1F];    // open the bucket with it
      for (int i=0; i < bucket.length; i += 2)
      {
         int j = bucket[i];
         if (j >= key)
         {
            if (j == key) return (char)bucket[i+1];
            break;
         }
      }
      return 0;  // which is an invalid unicode character
   }
}
