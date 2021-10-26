using Microsoft.WindowsAzure.Storage.Table;
using System;
using System.Collections.Generic;
using System.Text;

namespace CounterFunctions
{
    public class status : TableEntity
    {
        public string isOpen { get; set; }
    }
}