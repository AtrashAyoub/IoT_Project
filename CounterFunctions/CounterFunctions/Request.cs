using Microsoft.WindowsAzure.Storage.Table;
using System;
using System.Collections.Generic;
using System.Text;

namespace CounterFunctions
{
    public class Request : TableEntity
    {
        public string approved { get; set; }
        public string ownerName { get; set; }
        public string UserName { get; set; }

    }
}